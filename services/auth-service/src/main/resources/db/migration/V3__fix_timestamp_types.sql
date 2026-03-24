-- Hibernate 6 maps java.time.Instant to TIMESTAMP WITH TIME ZONE (timestamptz).
-- Alter all timestamp columns to TIMESTAMPTZ to pass ddl-auto: validate.

ALTER TABLE users
    ALTER COLUMN created_at TYPE TIMESTAMPTZ,
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ;

ALTER TABLE refresh_tokens
    ALTER COLUMN expires_at TYPE TIMESTAMPTZ,
    ALTER COLUMN created_at TYPE TIMESTAMPTZ;

ALTER TABLE oauth_accounts
    ALTER COLUMN created_at TYPE TIMESTAMPTZ;

ALTER TABLE password_reset_tokens
    ALTER COLUMN expires_at TYPE TIMESTAMPTZ,
    ALTER COLUMN created_at TYPE TIMESTAMPTZ;
