#!/usr/bin/env bash
# One-time consolidation of the six per-service databases into pitwall_db.
#
# Before the consolidation, each service owned its own database:
#
#   auth_db          users, refresh_tokens, oauth_accounts,
#                    password_reset_tokens, email_verification_tokens
#   league_db        leagues, league_members, scoring_configs
#   prediction_db    predictions, prediction_entries, bonus_bets
#   scoring_db       race_scores, league_standings
#   notification_db  device_tokens, notification_preferences
#   analytics_db     race_analytics_events, prediction_participation_stats
#
# pitwall-api reads a single pitwall_db instead. Its Flyway migrations are the
# six services' migrations renumbered -- byte-for-byte identical, verified with
# diff -- so pitwall_db's schema is exactly the union of the six, every table
# name is distinct, and no foreign key crosses what used to be a database
# boundary. That makes this a data-only copy with no transformation.
#
# Without it the old data stays on disk, unread: pitwall_db comes up empty and
# the app looks like a fresh install.
#
# Guarantees
#   - Idempotent. Each table is skipped if pitwall_db already holds rows for it,
#     so a re-run after a partial failure resumes instead of duplicating.
#   - Non-destructive. The source databases are only read. Nothing is dropped
#     here; drop them by hand once you are satisfied (see the end of this file).
#   - Fails loudly. Any copy error aborts with a non-zero exit, that database's
#     transaction rolled back.
#
# Run it by hand:  STACK_DIR=/opt/f1predict /opt/f1predict/consolidate-databases.sh
# Dry run (report only, change nothing):  DRY_RUN=1 ... /consolidate-databases.sh

set -euo pipefail

STACK_DIR="${STACK_DIR:-$HOME/f1predict}"
CONTAINER="${POSTGRES_CONTAINER:-f1predict-postgres-1}"
PGUSER="${PGUSER:-f1predict}"
TARGET_DB="${TARGET_DB:-pitwall_db}"
DRY_RUN="${DRY_RUN:-0}"

# Source database -> the tables it owns, in foreign-key-safe order (parents
# first). pg_dump --data-only emits tables alphabetically, which would put
# league_members before leagues; copying table by table in this order keeps
# every insert legal without disabling triggers.
LEGACY_DBS="auth_db league_db prediction_db scoring_db notification_db analytics_db"
tables_for() {
  case "$1" in
    auth_db)         echo "users refresh_tokens oauth_accounts password_reset_tokens email_verification_tokens" ;;
    league_db)       echo "leagues league_members scoring_configs" ;;
    prediction_db)   echo "predictions prediction_entries bonus_bets" ;;
    scoring_db)      echo "race_scores league_standings" ;;
    notification_db) echo "device_tokens notification_preferences" ;;
    analytics_db)    echo "race_analytics_events prediction_participation_stats" ;;
    *)               echo "" ;;
  esac
}

# Tables with a BIGSERIAL primary key. Copying rows does not advance the
# sequence, so the next insert would collide with an existing id. Each of these
# gets its sequence reset to max(id) afterwards.
SERIAL_TABLES="device_tokens notification_preferences race_analytics_events prediction_participation_stats"

log() { echo "[$(date -u +%FT%TZ)] $*"; }

# docker exec is how this reaches Postgres; the VPS has no psql of its own.
psql_run() { docker exec -i "$CONTAINER" psql -U "$PGUSER" -v ON_ERROR_STOP=1 "$@"; }
psql_val() { docker exec -i "$CONTAINER" psql -U "$PGUSER" -tAc "$2" -d "$1"; }

if ! docker inspect "$CONTAINER" >/dev/null 2>&1; then
  log "ERROR: container $CONTAINER not found -- is the stack running?"
  exit 1
fi

db_exists() {
  [ "$(psql_val postgres "SELECT count(*) FROM pg_database WHERE datname = '$1'")" = "1" ]
}

if ! db_exists "$TARGET_DB"; then
  log "ERROR: $TARGET_DB does not exist. Let the deploy create and migrate it first."
  exit 1
fi

# Flyway must have run: the tables have to be there before anything is copied.
APPLIED=$(psql_val "$TARGET_DB" \
  "SELECT count(*) FROM flyway_schema_history WHERE success" 2>/dev/null || echo 0)
if [ "${APPLIED:-0}" -lt 12 ]; then
  log "ERROR: $TARGET_DB has only ${APPLIED:-0} successful migrations (expected 12+)."
  log "       pitwall-api has not finished migrating yet. Wait for it and re-run."
  exit 1
fi
log "$TARGET_DB: $APPLIED migrations applied"

# ── Inventory ───────────────────────────────────────────────────────────────
# Printed whether or not anything is copied, so the deploy log always records
# what was on the box.
TOTAL_LEGACY=0
for db in $LEGACY_DBS; do
  if ! db_exists "$db"; then
    log "  $db: absent"
    continue
  fi
  counts=""
  for t in $(tables_for "$db"); do
    n=$(psql_val "$db" "SELECT count(*) FROM $t" 2>/dev/null || echo "?")
    counts="$counts $t=$n"
    case "$n" in ''|*[!0-9]*) ;; *) TOTAL_LEGACY=$((TOTAL_LEGACY + n)) ;; esac
  done
  log "  $db:$counts"
done
log "legacy rows found: $TOTAL_LEGACY"

if [ "$TOTAL_LEGACY" -eq 0 ]; then
  log "nothing to consolidate -- the old databases are empty or gone"
  exit 0
fi

# A duplicate invite code would have been legal before V12 added the UNIQUE
# constraint. Report it now rather than failing halfway through the copy.
if db_exists league_db; then
  DUPES=$(psql_val league_db "
    SELECT coalesce(string_agg(invite_code || ' x' || n, ', '), '')
    FROM (SELECT invite_code, count(*) n FROM leagues
          WHERE invite_code IS NOT NULL
          GROUP BY invite_code HAVING count(*) > 1) d")
  if [ -n "$DUPES" ]; then
    log "ERROR: league_db has duplicate invite codes, which $TARGET_DB now forbids:"
    log "       $DUPES"
    log "       Give each of those leagues a fresh code in league_db, then re-run."
    exit 1
  fi
fi

if [ "$DRY_RUN" = "1" ]; then
  log "DRY_RUN=1 -- stopping before any write"
  exit 0
fi

# ── Copy ────────────────────────────────────────────────────────────────────
COPIED=0
SKIPPED=0
for db in $LEGACY_DBS; do
  db_exists "$db" || continue

  for t in $(tables_for "$db"); do
    src=$(psql_val "$db" "SELECT count(*) FROM $t" 2>/dev/null || echo 0)
    [ "$src" -gt 0 ] || continue

    dst=$(psql_val "$TARGET_DB" "SELECT count(*) FROM $t")
    if [ "$dst" -gt 0 ]; then
      log "  skip $db.$t -- $TARGET_DB.$t already holds $dst rows"
      SKIPPED=$((SKIPPED + 1))
      continue
    fi

    log "  copy $db.$t ($src rows)"
    # COPY ... TO STDOUT / FROM STDIN streams the rows without a temp file and
    # without pg_dump's alphabetical table order. Column names are listed
    # explicitly on both sides so the copy does not depend on column position.
    cols=$(psql_val "$TARGET_DB" "
      SELECT string_agg(quote_ident(column_name), ',' ORDER BY ordinal_position)
      FROM information_schema.columns
      WHERE table_schema = 'public' AND table_name = '$t'")
    if ! docker exec -i "$CONTAINER" psql -U "$PGUSER" -v ON_ERROR_STOP=1 -d "$db" \
          -c "\\copy (SELECT $cols FROM $t) TO STDOUT" \
       | docker exec -i "$CONTAINER" psql -U "$PGUSER" -v ON_ERROR_STOP=1 -d "$TARGET_DB" \
          -c "\\copy $t ($cols) FROM STDIN"; then
      log "ERROR: copy of $db.$t failed -- stopping. $TARGET_DB.$t is unchanged."
      exit 1
    fi
    moved=$(psql_val "$TARGET_DB" "SELECT count(*) FROM $t")
    if [ "$moved" != "$src" ]; then
      log "ERROR: $t has $moved rows in $TARGET_DB but $src in $db -- stopping."
      exit 1
    fi
    COPIED=$((COPIED + 1))
  done
done

# ── Sequences ───────────────────────────────────────────────────────────────
for t in $SERIAL_TABLES; do
  n=$(psql_val "$TARGET_DB" "SELECT count(*) FROM $t")
  [ "$n" -gt 0 ] || continue
  psql_val "$TARGET_DB" "
    SELECT setval(pg_get_serial_sequence('$t', 'id'), (SELECT max(id) FROM $t))" >/dev/null
  log "  sequence for $t reset past $n rows"
done

log "consolidated: $COPIED tables copied, $SKIPPED already populated"
log ""
log "pitwall-api caches nothing across a restart, but restart it anyway so any"
log "in-flight request sees a consistent database:"
log "  docker compose -f docker-compose.yml -f docker-compose.prod.yml restart pitwall-api"
log ""
log "The old databases are untouched. Once the app looks right, reclaim them:"
for db in $LEGACY_DBS; do
  log "  docker exec $CONTAINER psql -U $PGUSER -d postgres -c 'DROP DATABASE $db'"
done
