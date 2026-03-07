CREATE TABLE IF NOT EXISTS campaign_signatures (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL UNIQUE REFERENCES advertising_campaigns(id) ON DELETE CASCADE,
    document_hash VARCHAR(64) NOT NULL,
    moderator_id BIGINT REFERENCES moderators(id),
    moderator_signed_at TIMESTAMP,
    client_id BIGINT REFERENCES clients(id),
    client_signed_at TIMESTAMP,
    fully_signed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_campaign_signatures_campaign_id ON campaign_signatures(campaign_id);
CREATE INDEX idx_campaign_signatures_fully_signed ON campaign_signatures(fully_signed);
