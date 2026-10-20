#!/usr/bin/env bash
# Daily Postgres backup for the Pitwall production stack (issue #150).
#
# Installed on the VPS by .github/workflows/deploy.yml and run from cron. Dumps
# both databases, keeps a rolling 7 days locally, and — when R2 credentials are
# present in .env — uploads a copy off the box.
#
# Run it by hand any time:  ~/f1predict/backup-db.sh
#
# A local-only backup lives on the same disk as the database it protects, so it
# covers the likely failure (a bad migration, a wrong DELETE) but not disk loss.
# Set the R2_* variables below in ~/f1predict/.env to get off-box copies.

set -euo pipefail

STACK_DIR="${STACK_DIR:-$HOME/f1predict}"
BACKUP_DIR="${BACKUP_DIR:-$STACK_DIR/backups}"
CONTAINER="${POSTGRES_CONTAINER:-f1predict-postgres-1}"
PGUSER="${PGUSER:-f1predict}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
DATABASES="${DATABASES:-pitwall_db f1data_db}"

mkdir -p "$BACKUP_DIR"
STAMP=$(date -u +%Y%m%dT%H%M%SZ)

log() { echo "[$(date -u +%FT%TZ)] $*"; }

# Optional off-box upload. Set in ~/f1predict/.env:
#   R2_BUCKET, R2_ENDPOINT, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY
if [ -f "$STACK_DIR/.env" ]; then
  # shellcheck disable=SC1091
  set -a; . "$STACK_DIR/.env"; set +a
fi

if ! docker inspect "$CONTAINER" >/dev/null 2>&1; then
  log "ERROR: container $CONTAINER not found — is the stack running?"
  exit 1
fi

FAILED=0
for db in $DATABASES; do
  out="$BACKUP_DIR/${db}-${STAMP}.sql.gz"
  log "dumping $db"
  # pipefail is set, so a pg_dump failure fails the pipeline even though gzip
  # exits 0 — without it a dump error would leave a valid-looking empty archive.
  if ! docker exec "$CONTAINER" pg_dump -U "$PGUSER" --no-owner "$db" \
        2>>"$BACKUP_DIR/backup.log" | gzip > "$out"; then
    log "ERROR: pg_dump failed for $db — removing partial file"
    rm -f "$out"; FAILED=1; continue
  fi
  log "  wrote $out ($(du -h "$out" | cut -f1))"

  if [ -n "${R2_BUCKET:-}" ] && [ -n "${R2_ENDPOINT:-}" ]; then
    log "  uploading to R2"
    # Run the AWS CLI from a container so the VPS needs nothing installed.
    if docker run --rm \
        -v "$BACKUP_DIR:/backups:ro" \
        -e AWS_ACCESS_KEY_ID="${R2_ACCESS_KEY_ID:-}" \
        -e AWS_SECRET_ACCESS_KEY="${R2_SECRET_ACCESS_KEY:-}" \
        -e AWS_DEFAULT_REGION=auto \
        amazon/aws-cli:latest \
        s3 cp "/backups/$(basename "$out")" "s3://$R2_BUCKET/postgres/$(basename "$out")" \
        --endpoint-url "$R2_ENDPOINT" >>"$BACKUP_DIR/backup.log" 2>&1; then
      log "  uploaded"
    else
      log "  WARNING: R2 upload failed (local copy kept) — see backup.log"
      FAILED=1
    fi
  fi
done

log "pruning local dumps older than ${RETENTION_DAYS} days"
find "$BACKUP_DIR" -name '*.sql.gz' -type f -mtime "+${RETENTION_DAYS}" -print -delete | sed 's/^/  removed /' || true

if [ "$FAILED" -ne 0 ]; then
  log "completed WITH ERRORS"
  exit 1
fi
log "completed OK"
