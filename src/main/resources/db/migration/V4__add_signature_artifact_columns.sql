-- Adds nullable columns to store a real signature artifact fetched from the ЭДО operator.
-- These remain NULL until a provider actually returns artifact bytes (the stub does not).

ALTER TABLE campaign_signatures
    ADD COLUMN signature_artifact BYTEA,
    ADD COLUMN signature_artifact_content_type VARCHAR(128),
    ADD COLUMN signature_artifact_filename VARCHAR(255);
