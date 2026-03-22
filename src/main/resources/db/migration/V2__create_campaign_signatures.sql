CREATE TABLE IF NOT EXISTS campaign_signatures (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL UNIQUE REFERENCES advertising_campaigns(id) ON DELETE CASCADE,
    document_hash VARCHAR(64) NOT NULL,
    hash_algorithm VARCHAR(32) NOT NULL,
    signature_type VARCHAR(64) NOT NULL,
    document_template_version VARCHAR(64) NOT NULL,
    document_snapshot TEXT NOT NULL,
    moderator_id BIGINT REFERENCES moderators(id),
    moderator_signed_at TIMESTAMP,
    moderator_signed_at_utc TIMESTAMP WITH TIME ZONE,
    moderator_evidence TEXT,
    client_id BIGINT REFERENCES clients(id),
    client_signed_at TIMESTAMP,
    client_signed_at_utc TIMESTAMP WITH TIME ZONE,
    client_evidence TEXT,
    fully_signed BOOLEAN NOT NULL DEFAULT FALSE,
    fully_signed_at_utc TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_campaign_signatures_campaign_id ON campaign_signatures(campaign_id);
CREATE INDEX idx_campaign_signatures_fully_signed ON campaign_signatures(fully_signed);

CREATE TABLE IF NOT EXISTS campaign_signature_audit_events (
    id BIGSERIAL PRIMARY KEY,
    signature_id BIGINT NOT NULL REFERENCES campaign_signatures(id) ON DELETE CASCADE,
    event_type VARCHAR(64) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    actor_id BIGINT,
    actor_name VARCHAR(255),
    evidence_json TEXT,
    occurred_at TIMESTAMP NOT NULL,
    occurred_at_utc TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_sig_audit_signature_id ON campaign_signature_audit_events(signature_id);
