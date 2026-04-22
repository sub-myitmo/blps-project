ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

ALTER TABLE moderators
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

ALTER TABLE comments
    ALTER COLUMN moderator_id DROP NOT NULL;

CREATE TABLE IF NOT EXISTS user_accounts (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    client_id BIGINT REFERENCES clients(id),
    moderator_id BIGINT REFERENCES moderators(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_accounts_owner CHECK (
        (role = 'CLIENT' AND client_id IS NOT NULL AND moderator_id IS NULL)
        OR (role = 'MANAGER' AND moderator_id IS NOT NULL AND client_id IS NULL)
        OR (role = 'ADMIN' AND client_id IS NULL AND moderator_id IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_user_accounts_username ON user_accounts(username);
CREATE INDEX IF NOT EXISTS idx_user_accounts_role ON user_accounts(role);
CREATE INDEX IF NOT EXISTS idx_user_accounts_client_id ON user_accounts(client_id);
CREATE INDEX IF NOT EXISTS idx_user_accounts_moderator_id ON user_accounts(moderator_id);

ALTER TABLE campaign_signatures
    ADD COLUMN IF NOT EXISTS pdf_object_key VARCHAR(512),
    ADD COLUMN IF NOT EXISTS pdf_content_type VARCHAR(128),
    ADD COLUMN IF NOT EXISTS pdf_created_at_utc TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS pdf_status VARCHAR(50) NOT NULL DEFAULT 'NOT_CREATED';

CREATE INDEX IF NOT EXISTS idx_campaign_signatures_pdf_object_key
    ON campaign_signatures(pdf_object_key);
