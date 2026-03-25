# Electronic Signature Implementation

## 1. Overview

This project implements an electronic-signature workflow for advertising campaigns.
The workflow is centered around one rule: the backend freezes campaign terms into a deterministic document snapshot, sends that frozen document to an external EDO operator, and uses operator state plus local audit evidence to decide when the campaign is legally ready to proceed.

The backend does **not** generate cryptographic signatures itself. The legal signing responsibility is delegated to the EDO operator behind the `EdoOperatorClient` interface.

In the current codebase:

- the default runtime path uses an in-memory stub EDO operator;
- the `diadoc-prod` profile contains only a skeleton integration and is not fully implemented;
- the signature lifecycle is attached to `AdvertisingCampaign`;
- each campaign has at most one `CampaignSignature` record, reused across signing cycles.

## 2. Main Design Principles

### 2.1 Document freeze before signing

At moderator signing time, the system creates a text snapshot of the campaign and stores it in `campaign_signatures.document_snapshot`.

That snapshot becomes the source of truth for the signing cycle.

### 2.2 Hash-based integrity check

The snapshot is hashed with SHA-256 and stored in `campaign_signatures.document_hash`.

Before client countersigning, the backend recomputes the hash from the current campaign state and compares it with the frozen hash. If they differ, the signing flow is aborted with `Campaign document hash mismatch`.

This protects against post-freeze changes to campaign terms.

### 2.3 Operator-mediated signing

The backend sends the frozen document to an EDO operator and stores:

- operator name;
- message ID;
- entity ID;
- operator document status;
- certificate thumbprints;
- raw operator payload.

The backend treats the operator as the source of truth for the cryptographic signing result.

### 2.4 Audit trail as evidence

Every important signature event is captured as a `CampaignSignatureAuditEvent` with:

- event type;
- actor type;
- actor ID and actor name;
- JSON evidence payload;
- timestamp in `Instant` and `LocalDateTime`.

## 3. Key Files

- `src/main/java/ru/aviasales/service/ModeratorService.java`
- `src/main/java/ru/aviasales/service/ClientService.java`
- `src/main/java/ru/aviasales/service/CampaignEdoSyncService.java`
- `src/main/java/ru/aviasales/service/CampaignSignatureAuditService.java`
- `src/main/java/ru/aviasales/service/CampaignSigningDocumentFactory.java`
- `src/main/java/ru/aviasales/service/CampaignDocumentHashUtil.java`
- `src/main/java/ru/aviasales/service/edo/EdoOperatorClient.java`
- `src/main/java/ru/aviasales/service/edo/StubEdoOperatorClient.java`
- `src/main/java/ru/aviasales/service/edo/DiadocEdoOperatorClient.java`
- `src/main/java/ru/aviasales/dal/model/CampaignSignature.java`
- `src/main/java/ru/aviasales/dal/model/CampaignSignatureAuditEvent.java`
- `src/main/resources/db/migration/V2__create_campaign_signatures.sql`

## 4. Data Model

### 4.1 `campaign_signatures`

The `CampaignSignature` entity is a one-to-one extension of `AdvertisingCampaign`.

Main fields:

| Field | Purpose |
| --- | --- |
| `campaign_id` | One signature aggregate per campaign |
| `document_hash` | SHA-256 hash of the frozen snapshot |
| `hash_algorithm` | Currently `SHA-256` |
| `document_template_version` | Snapshot format version, currently `v1` |
| `document_snapshot` | Frozen text representation of campaign terms |
| `moderator_id` | Moderator who initiated the cycle |
| `moderator_signed_at`, `moderator_signed_at_utc` | Moderator signing confirmation timestamps |
| `moderator_evidence` | JSON evidence for moderator-side confirmation |
| `client_id` | Client who initiated countersign |
| `client_signed_at`, `client_signed_at_utc` | Client signing confirmation timestamps |
| `client_evidence` | JSON evidence for client-side confirmation |
| `fully_signed` | Final completion flag |
| `fully_signed_at_utc` | Completion timestamp |
| `edo_operator` | Canonical operator name |
| `edo_message_id`, `edo_entity_id` | External operator identifiers |
| `edo_document_status` | Canonical operator-side status |
| `edo_moderator_box_id`, `edo_client_box_id` | Sender and recipient box IDs |
| `edo_moderator_cert_thumbprint`, `edo_client_cert_thumbprint` | Certificate thumbprints received from operator |
| `edo_last_synced_at_utc` | Last poll time |
| `edo_raw_response` | Latest raw payload from operator |

Important notes:

- `signature_type` exists in the schema and entity, but the current service flow sets it to `null`.
- old signature-cycle state is explicitly reset when moderator starts a fresh signing cycle.

### 4.2 `campaign_signature_audit_events`

Each `CampaignSignature` has many ordered audit events.

Main fields:

| Field | Purpose |
| --- | --- |
| `signature_id` | Parent signature aggregate |
| `event_type` | Business event type |
| `actor_type` | `MODERATOR`, `CLIENT`, or `SYSTEM` |
| `actor_id`, `actor_name` | Optional actor identity |
| `evidence_json` | Serialized evidence payload |
| `occurred_at` | Legacy UTC `LocalDateTime` |
| `occurred_at_utc` | Precise UTC `Instant` |

Events are ordered ascending by `occurredAtUtc`.

## 5. Frozen Document Format

The frozen document is produced by `CampaignSigningDocumentFactory.buildSnapshot(campaign)`.

Current format:

```text
templateVersion=v1
campaignId=<campaign id>
campaignName=<campaign name>
content=<campaign content>
targetUrl=<target url>
dailyBudget=<budget>
startDate=<start date in LocalDate string form, YYYY-MM-DD>
endDate=<end date in LocalDate string form, YYYY-MM-DD>
clientId=<client id>
```

Properties of this approach:

- deterministic ordering of fields;
- newline-separated key-value layout;
- null values converted to empty string where applicable;
- template version stored both in the snapshot and in the database.

Hashing is done by `CampaignDocumentHashUtil` with SHA-256 over UTF-8 bytes.

## 6. End-to-End Workflow

### 6.1 Campaign creation

The client creates a campaign through `ClientService.createCampaign(...)`.

Initial state:

- campaign status = `PENDING`;
- no signature row yet.

### 6.2 Moderator starts signing

Endpoint:

- `POST /api/moderator/campaigns/{id}`

Request:

```json
{
  "action": "SIGN_DOC",
  "comment": "Campaign reviewed and sent for signing",
  "consentAccepted": true
}
```

Processing in `ModeratorService.actionCampaign(...)`:

1. Load moderator by API key.
2. Lock campaign with `findByIdForUpdate(...)`.
3. Require campaign status `PENDING`.
4. Validate consent.
5. Create or reuse `CampaignSignature`.
6. Build frozen snapshot and SHA-256 hash.
7. Store snapshot metadata on the signature.
8. Clear previous audit events.
9. Reset moderator/client/operator state for a fresh cycle.
10. Save the signature.
11. Send document to the EDO operator with sender and recipient box IDs.
12. Store returned operator identifiers and operator-side status.
13. Record `DOCUMENT_FROZEN`.
14. Record `EDO_DOCUMENT_SENT`.
15. Transition campaign from `PENDING` to `AT_SIGNING`.

The moderator does not mark the signature as completed locally at this step. Completion depends on later confirmation from the operator.

### 6.3 Operator sync after moderator send

Sync is handled by `CampaignEdoSyncService.trySync(...)`.

It runs opportunistically from:

- `ModeratorService.getCampaign(...)`;
- `ModeratorService.getCampaignSignature(...)`;
- `ClientService.getCampaign(...)`;
- `ClientService.getCampaignSignature(...)`;
- `ClientService.actionCampaign(...)` before client countersigning.

Important behavior:

- sync is skipped if there is no operator message/entity ID;
- sync is skipped if the previous sync happened less than 10 seconds ago;
- there is no dedicated scheduled EDO polling job for this signature flow in the current implementation.

If the operator says the moderator-side signature is confirmed:

- `MODERATOR_SIGN_CONFIRMED` audit event is recorded once;
- `moderator_signed_at` and `moderator_signed_at_utc` are filled;
- `moderator_evidence` is stored.

### 6.4 Client countersigns

Endpoint:

- `POST /api/client/campaigns/{id}`

Recommended request:

```json
{
  "action": "SIGN_DOC",
  "consentAccepted": true,
  "documentHash": "<hash from GET /signature>"
}
```

Processing in `ClientService.actionCampaign(...)`:

1. Lock campaign with `findByIdForUpdate(...)`.
2. Validate that the caller owns the campaign.
3. Require campaign status `AT_SIGNING`.
4. Validate consent.
5. Load existing signature.
6. Trigger sync before continuing.
7. If request contains `documentHash`, require it to match the stored hash.
8. Recompute hash from the current campaign and compare with frozen hash.
9. Ensure operator identifiers exist.
10. Ensure moderator-side signing is confirmed by operator status.
11. Ensure countersign is not already in progress or completed.
12. Ensure current operator status allows countersign initiation.
13. Call `edoOperatorClient.initiateCounterSign(...)`.
14. Store updated operator status, client box ID, thumbprint, and raw response.
15. Store `client_id`.
16. Record `EDO_COUNTERSIGN_INITIATED`.

At this point the campaign is still not fully signed. Finalization happens only after a later sync sees the operator-side completion.

### 6.5 Final completion

When `CampaignEdoSyncService.sync(...)` sees `EdoDocumentStatus.COMPLETED` and `fullySigned == false`, it:

1. Records `CLIENT_SIGN_CONFIRMED`.
2. Stores client signing timestamps.
3. Stores `client_evidence`.
4. Sets `fully_signed = true`.
5. Sets `fully_signed_at_utc`.
6. Records `SIGNATURE_COMPLETED` once.
7. Transitions campaign from `AT_SIGNING` to `WAITING_START`.

This means campaign activation readiness is gated by both parties being confirmed by the operator.

## 7. EDO Status Model

Canonical statuses are defined in `EdoDocumentStatus`:

| Status | Meaning |
| --- | --- |
| `RECIPIENT_SIGNATURE_REQUIRED` | Moderator side is already accepted; client signature is required |
| `COUNTERSIGN_INITIATED` | Client countersign request was initiated |
| `COMPLETED` | Both sides are completed |
| `SENDER_SIGNED` | Legacy/provider-specific synonym for moderator completion |
| `RECIPIENT_SIGNED` | Legacy/provider-specific intermediate recipient state |

Helper rules:

- `isModeratorConfirmed(status)` returns true for all states at or after moderator confirmation.
- `canInitiateCountersign(status)` returns true for `RECIPIENT_SIGNATURE_REQUIRED` and `SENDER_SIGNED`.
- `isCountersignInProgress(status)` returns true for `COUNTERSIGN_INITIATED` and `RECIPIENT_SIGNED`.
- `isCompleted(status)` returns true only for `COMPLETED`.

## 8. Stub Operator Behavior

The active non-production implementation is `StubEdoOperatorClient`.

Behavior:

1. `sendDocumentForSigning(...)`
   - generates random `messageId` and `entityId`;
   - immediately treats the moderator side as signed;
   - returns `RECIPIENT_SIGNATURE_REQUIRED`.

2. `initiateCounterSign(...)`
   - validates that the document is ready for countersign;
   - switches the internal state to `COUNTERSIGN_INITIATED`.

3. `getSigningStatus(...)`
   - if the internal state is `COUNTERSIGN_INITIATED`, it auto-completes the document to `COMPLETED`;
   - returns sender and recipient timestamps and stub certificate thumbprints.

Practical consequence:

- in stub mode, client signing does not complete immediately inside the countersign request itself;
- completion happens on the next sync-triggering read or action.

## 9. Production Diadoc Boundary

`DiadocEdoOperatorClient` is only a placeholder at the moment.

What already exists:

- Spring profile gate: `diadoc-prod`;
- injected `RestTemplate`;
- configurable `edo.diadoc.api-url`;
- configurable `edo.diadoc.api-key`;
- documented integration boundary.

What is still missing:

- real Diadoc authentication flow;
- real request/response mapping;
- canonical status mapping;
- real sender/recipient signature exchange;
- stable parsing of signing timestamps and certificate metadata.

Today, enabling `diadoc-prod` will result in `UnsupportedOperationException` for all EDO operations.

## 10. Audit Events and Evidence

`CampaignSignatureAuditService` builds evidence JSON with these keys:

- `eventType`
- `actorType`
- `actorId`
- `actorName`
- `consentStatement` when available
- `occurredAtUtc`
- `edoOperator` when EDO data is available
- `edoMessageId`
- `edoEntityId`
- `edoStatus`
- `edoCertThumbprint` when available

Currently recorded event types in the actual flow:

- `DOCUMENT_FROZEN`
- `EDO_DOCUMENT_SENT`
- `MODERATOR_SIGN_CONFIRMED`
- `EDO_COUNTERSIGN_INITIATED`
- `CLIENT_SIGN_CONFIRMED`
- `SIGNATURE_COMPLETED`

Important nuance:

- `MODERATOR_SIGNED` and `CLIENT_SIGNED` exist in the enum but are not emitted by the current services.

## 11. API Surface

### 11.1 Authentication

Authentication is based on the `Authorization` header and enforced by `AuthInterceptor`.

- `/api/client/**` and `/api/payment/**` require a valid client API key;
- `/api/moderator/**` require a valid moderator API key.

Even read endpoints that do not explicitly declare the header in the controller are still protected by the interceptor.

### 11.2 Main signature-related endpoints

Moderator side:

- `POST /api/moderator/campaigns/{id}` with `SIGN_DOC`
- `GET /api/moderator/campaigns/{id}`
- `GET /api/moderator/campaigns/{id}/signature`

Client side:

- `POST /api/client/campaigns/{id}` with `SIGN_DOC`
- `GET /api/client/campaigns/{id}`
- `GET /api/client/campaigns/{id}/signature`

Response models:

- `CampaignResponse` contains a signature summary;
- `CampaignSignatureDetailsResponse` contains full snapshot, timestamps, operator metadata, and ordered audit events.

## 12. State and Consistency Guarantees

### 12.1 Pessimistic locking

Both moderator and client action methods load the campaign with `findByIdForUpdate(...)`, which uses `PESSIMISTIC_WRITE`.

This reduces race conditions around concurrent state transitions.

### 12.2 Immutable signed terms

The signing flow protects campaign terms by:

- freezing the full snapshot before sending to the operator;
- verifying the hash before client countersign;
- forbidding signed date changes on campaign resume if the campaign already has a fully signed signature.

### 12.3 Single signature aggregate per campaign

`campaign_id` is unique in `campaign_signatures`.

Instead of creating a new row for every cycle, the current implementation reuses the same `CampaignSignature` row and resets its fields for a fresh signing cycle.

### 12.4 Audit history reset per cycle

When moderator starts a new cycle, `signature.getAuditEvents().clear()` is called.

Because the association uses `orphanRemoval = true`, old audit events are deleted as part of the new cycle.

This means the database keeps only the latest cycle's audit trail, not a permanent history of all cycles.

## 13. Configuration

Relevant application properties:

```properties
edo.moderator-box-id=${EDO_MODERATOR_BOX_ID:stub-moderator-box}
edo.client-box-id=${EDO_CLIENT_BOX_ID:stub-client-box}
edo.diadoc.api-url=${EDO_DIADOC_API_URL:https://diadoc-api.kontur.ru}
edo.diadoc.api-key=${EDO_DIADOC_API_KEY:}
```

Profiles:

- default or any profile except `diadoc-prod`: stub operator is active;
- `diadoc-prod`: real Diadoc client bean is selected, but still not implemented.

## 14. Current Limitations and Caveats

1. The backend never creates the actual cryptographic signature. It only orchestrates the process and stores evidence.
2. The production Diadoc client is a stubbed boundary, not a working integration.
3. `signature_type` is present in schema but unused in the active service flow.
4. `consentAccepted` is only rejected when it is explicitly `false`; `null` currently passes validation because the DTO fields are nullable and the service checks `Boolean.FALSE.equals(...)`.
5. There is no dedicated scheduled poller for signature completion; the flow depends on reads or actions that trigger `trySync(...)`.
6. Starting a fresh signing cycle erases prior cycle audit events.
7. I did not find signature-specific automated tests under `src/test/java` in the current workspace.

## 15. Recommended Next Steps for Hardening

1. Implement the real `DiadocEdoOperatorClient`.
2. Make `consentAccepted` mandatory with `@NotNull` on both action DTOs.
3. Decide whether `signature_type` should be populated or removed.
4. Add integration tests for moderator sign, client countersign, hash mismatch, and cycle reset.
5. Decide whether previous signing cycles must be preserved instead of overwritten.
6. Add an explicit background sync strategy if operator confirmation should not depend on client or moderator reads.
