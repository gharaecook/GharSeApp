CREATE TABLE password_reset_token (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_password_reset_token_user ON password_reset_token (user_id);

ALTER TABLE users ADD COLUMN credential_version INT NOT NULL DEFAULT 0;
