-- Simplify campaign_signatures: remove EDO operator fields, audit events, artifacts

DROP TABLE IF EXISTS campaign_signature_audit_events;

ALTER TABLE campaign_signatures
    DROP COLUMN IF EXISTS signature_type,
    DROP COLUMN IF EXISTS moderator_signed_at,
    DROP COLUMN IF EXISTS moderator_evidence,
    DROP COLUMN IF EXISTS client_signed_at,
    DROP COLUMN IF EXISTS client_evidence,
    DROP COLUMN IF EXISTS edo_operator,
    DROP COLUMN IF EXISTS edo_message_id,
    DROP COLUMN IF EXISTS edo_entity_id,
    DROP COLUMN IF EXISTS edo_document_status,
    DROP COLUMN IF EXISTS edo_moderator_box_id,
    DROP COLUMN IF EXISTS edo_client_box_id,
    DROP COLUMN IF EXISTS edo_moderator_cert_thumbprint,
    DROP COLUMN IF EXISTS edo_client_cert_thumbprint,
    DROP COLUMN IF EXISTS edo_last_synced_at_utc,
    DROP COLUMN IF EXISTS edo_raw_response,
    DROP COLUMN IF EXISTS signature_artifact,
    DROP COLUMN IF EXISTS signature_artifact_content_type,
    DROP COLUMN IF EXISTS signature_artifact_filename;

DROP INDEX IF EXISTS idx_campaign_signatures_edo_status;
