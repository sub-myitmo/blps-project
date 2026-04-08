# Signature Flow: Advertising Campaign

## Bug Report Analysis: "Failed to fetch" in Swagger UI

### Summary

| Layer | Status | Finding |
|-------|--------|---------|
| Browser / Swagger UI | **Root cause of "Failed to fetch"** | SSH tunnel dropped (ECONNREFUSED → TypeError) |
| fetch() / network | Not the issue | Node.js `fetch()` returns HTTP 200 when tunnel is up |
| Backend controller | **Bug** | Request reaches backend and returns 200 — should return 400 |
| Bean Validation | **Bug** | `@Pattern(regexp = "^(http|https)://.*$")` accepts any garbage after `http://` |
| Service layer | Not reached for this error | createCampaign succeeds and stores the malformed URL |
| Signature layer | **Downstream impact** | Malformed URL gets frozen into the signed document |

---

### 1. Root Cause

**"Failed to fetch" = SSH tunnel is not running.**

The Swagger UI page is served through an SSH tunnel (`ssh -L 18080:localhost:18080 ...`). When the tunnel drops (no keepalive configured), the browser's `fetch()` gets `ECONNREFUSED`, which Swagger UI displays as "Failed to fetch". This is a **network connectivity error**, not a validation error.

**Evidence:**
- `curl POST` with the exact malformed payload → **HTTP 200** (backend receives and processes it)
- Node.js `fetch()` with same payload → **HTTP 200** when tunnel is up; **TypeError: fetch failed (ECONNREFUSED)** when tunnel is down
- The malformed URL characters (`<`, `#`, `'`, ` `, `$`, `*`) do NOT cause any browser-level fetch rejection

### 2. The Real Backend Bug

The URL passes validation when it should not:

```
targetUrl = "http://bIC:Mi<H: g*LDnH.ab9Z$W-ev'I+B#C,,< e+.Wa"
```

**Regex `^(http|https)://.*$`** — only checks the scheme prefix. The rest can be any string.  
**Result:** HTTP 200, campaign created, malformed URL stored.

**Fix applied:** Replaced `@Pattern` with Hibernate's `@URL` on both `CampaignRequest` and `UpdateCampaignRequest`. `@URL` uses `java.net.URL` internally and rejects non-parseable URLs.

### 3. What the Response Looks Like vs What Was Expected

| Scenario | Before fix | After fix |
|----------|-----------|-----------|
| Malformed URL | HTTP 200 — campaign created | HTTP 400 with field error |
| Missing required field | HTTP 400 — no field detail (Spring default) | HTTP 400 with `{"errors": [{"field": "name", "message": "..."}]}` |
| URL with wrong scheme (ftp://) | HTTP 400 | HTTP 400 |

**Secondary fix:** Added `GlobalExceptionHandler` (`@RestControllerAdvice`) to return field-level validation messages instead of Spring Boot's bare `{"status":400,"error":"Bad Request"}`.

### 4. Exact Layer Where "Failed to fetch" Happens

```
Browser (Swagger UI page at http://localhost:18080/swagger-ui/index.html)
  └── fetch('http://localhost:18080/api/client/campaigns', {...})
        └── TCP connect to 127.0.0.1:18080
              └── SSH tunnel (local side) ← DROPS HERE
                    └── SSH tunnel (remote side → app at :18080)
```

The failure is at the TCP layer. The request never reaches the application server.

**Fix:** Restart tunnel with keepalive:
```sh
ssh -L 18080:localhost:18080 s412939@helios.cs.ifmo.ru -p 2222 -N -f \
  -o ServerAliveInterval=30 -o ServerAliveCountMax=6
```

---

## Signature Flow: End-to-End Reference

### Actors

| Actor | Role | API key header |
|-------|------|---------------|
| **Client** | Campaign owner. Creates, submits, signs. | `Authorization: <client-api-key>` |
| **Moderator** | Reviews, approves, signs first. | `Authorization: <moderator-api-key>` |
| **System** | Builds document snapshot and hash; enforces state machine. | — |

---

### Preconditions

- Client exists in `clients` table with a valid `api_key`.
- Moderator exists in `moderators` table with a valid `api_key`.
- Campaign `targetUrl` is a well-formed `http://` or `https://` URL.
- Campaign `startDate` is not in the past; `endDate` (if set) is after `startDate`.

---

### Status State Machine

```
                   [create]
                      │
                   PENDING
                  /       \
             [SIGN_DOC]   [REJECT]
                /               \
          AT_SIGNING          REJECTED ──[re-submit]──> PENDING
              │
          [client SIGN_DOC]
              │
         WAITING_START
           /       \
      [start]     [freeze: no funds]
        /               \
     ACTIVE           FROZEN
    / | \               │
   /  |  \          [resume]
PAUSED_BY_CLIENT  WAITING_START
PAUSED_BY_MOD
COMPLETED
```

Allowed transitions (enforced by `CampaignStatus.canTransitionTo()`):

| From | To | Who |
|------|----|-----|
| PENDING | AT_SIGNING | Moderator (`SIGN_DOC`) |
| PENDING | REJECTED | Moderator (`REJECT`) |
| AT_SIGNING | WAITING_START | Client (`SIGN_DOC`) |
| PAUSED_BY_CLIENT / PAUSED_BY_MODERATOR / FROZEN / AT_SIGNING | WAITING_START | Client (`RESUME`) |
| WAITING_START | ACTIVE | System (scheduler) |
| WAITING_START | FROZEN | System (no funds) |
| ACTIVE | PAUSED_BY_CLIENT | Client (`PAUSE`) |
| ACTIVE | PAUSED_BY_MODERATOR | Moderator (`PAUSE`) |
| ACTIVE | COMPLETED | System (end date passed) |
| ACTIVE | FROZEN | System (no funds) |
| REJECTED | PENDING | System (on update) |

Any other transition throws `RuntimeException("Действие недоступно")`.

---

### Normal Success Scenario

#### Step 1 — Client creates campaign

```
POST /api/client/campaigns
Authorization: client-key-1

{
  "name": "Summer Sale",
  "content": "Get 50% off all flights this summer!",
  "targetUrl": "https://aviasales.ru/promo/summer",
  "dailyBudget": 5000.00,
  "startDate": "2026-05-01",
  "endDate": "2026-07-31"
}
```

- Status set to `PENDING`.
- No signature record created yet.
- `documentHash` in response is `null`.

#### Step 2 — Moderator reviews and signs

```
POST /api/moderator/campaigns/{id}
Authorization: moderator-key-1

{
  "action": "SIGN_DOC",
  "consentAccepted": true,
  "comment": "Checked: complies with advertising policy."
}
```

- **Document frozen at this point.** The system calls `CampaignSigningDocumentFactory.buildSnapshot(campaign)`, which captures:
  ```
  templateVersion=v1
  campaignId=42
  campaignName=Summer Sale
  content=Get 50% off all flights this summer!
  targetUrl=https://aviasales.ru/promo/summer
  dailyBudget=5000.00
  startDate=2026-05-01
  endDate=2026-07-31
  clientId=1
  ```
- SHA-256 hash of the snapshot is computed and stored in `CampaignSignature.documentHash`.
- `moderatorId` and `moderatorSignedAtUtc` are recorded.
- Status transitions `PENDING → AT_SIGNING`.

#### Step 3 — Client retrieves the document

```
GET /api/client/campaigns/{id}/signature
Authorization: client-key-1
```

Returns `CampaignSignatureDetailsResponse` including:
- `documentHash` — SHA-256 of the frozen snapshot
- `documentSnapshot` — the plain-text snapshot the moderator signed
- `documentTemplateVersion` — `"v1"`
- `moderatorSignedAtUtc` — timestamp

Client must verify `documentHash` matches the snapshot before signing.

#### Step 4 — Client downloads the PDF (optional)

```
GET /api/client/campaigns/{id}/signature/pdf
Authorization: client-key-1
```

Returns a PDF rendered from the frozen snapshot. Client can review all frozen fields.

#### Step 5 — Client signs

```
POST /api/client/campaigns/{id}
Authorization: client-key-1

{
  "action": "SIGN_DOC",
  "consentAccepted": true,
  "documentHash": "a3f2e1..."   ← optional; if provided must match stored hash
}
```

- **Integrity re-verified**: `CampaignDocumentHashUtil.buildDocumentHash(campaign)` is recomputed from the live campaign and compared with `signature.documentHash`. A mismatch means the campaign was mutated after the moderator signed — throws `RuntimeException("Campaign document hash mismatch")`.
- `clientId`, `clientSignedAtUtc`, `fullySigned=true`, `fullySignedAtUtc` are recorded.
- Status transitions `AT_SIGNING → WAITING_START`.

#### Step 6 — System activates campaign

When the scheduler detects `startDate <= today` and `status = WAITING_START`, it transitions to `ACTIVE`.

---

### Failure Scenarios

#### F1 — Malformed `targetUrl` (before fix)
- **Symptom**: HTTP 200 returned, campaign created with garbage URL.
- **Impact**: Malformed URL gets frozen into the signing document. Both parties sign a document containing an invalid URL. PDF renders the malformed URL as plain text.
- **Fix**: `@URL` constraint on `targetUrl` in `CampaignRequest` and `UpdateCampaignRequest`.

#### F2 — Moderator tries to sign a non-PENDING campaign
- **Endpoint**: `POST /api/moderator/campaigns/{id}` with `action=SIGN_DOC`
- **Condition**: Status ≠ PENDING
- **Response**: HTTP 400 `{"error": "Campaign must be in PENDING status for moderator signing"}`

#### F3 — Client tries to sign without moderator having signed first
- **Condition**: `CampaignSignature` not found for the campaign
- **Response**: HTTP 400 `{"error": "Campaign is not signed by moderator"}`

#### F4 — Document hash mismatch during client signing
- **Condition**: Campaign fields were mutated after moderator signed (direct DB edit, or a bug re-saving the campaign)
- **Response**: HTTP 400 `{"error": "Campaign document hash mismatch"}`
- **Note**: This protects against illegal mutations to frozen campaign data. Normal flow never hits this.

#### F5 — Client provides wrong `documentHash`
- **Condition**: `request.documentHash` is non-null, non-blank, and does not match `signature.documentHash`
- **Response**: HTTP 400 `{"error": "Document hash confirmation mismatch"}`

#### F6 — Client tries to RESUME a signed campaign with changed dates
- **Condition**: `signature.isFullySigned() == true` and dates in request differ from campaign dates
- **Response**: HTTP 400 `{"error": "Signed campaign terms cannot be changed without a new signing cycle"}`
- **Why**: Signed documents must not have their material terms silently changed. A new signing cycle is required.

#### F7 — Insufficient funds: ACTIVE → FROZEN
- Campaign balance runs out during daily deduction
- Status transitions `ACTIVE → FROZEN`
- Client must `RESUME` after topping up balance

#### F8 — Swagger UI shows "Failed to fetch"
- **Root cause**: SSH tunnel to helios is down
- **Fix**: Restart with `ssh -L 18080:localhost:18080 s412939@helios.cs.ifmo.ru -p 2222 -N -f -o ServerAliveInterval=30 -o ServerAliveCountMax=6`
- **Check**: `nc -z localhost 18080` — if fails, tunnel is down

---

### What Is Frozen Before Signing

The document snapshot (frozen at moderator `SIGN_DOC`) contains:

| Field | Description | Immutable after freeze? |
|-------|-------------|------------------------|
| `campaignId` | DB primary key | Yes |
| `campaignName` | Human-readable name | Yes |
| `content` | Ad copy text | Yes |
| `targetUrl` | Destination URL | Yes |
| `dailyBudget` | Daily spend limit | Yes |
| `startDate` | Campaign start date | Yes |
| `endDate` | Campaign end date | Yes |
| `clientId` | Owning client | Yes |
| `templateVersion` | Snapshot format version | Yes |

**Fields NOT frozen** (not in snapshot):
- `status` — transitions freely
- `comments` — moderator comments added separately
- `createdAt` — immutable but not in snapshot

After the moderator signs, the system hash-checks the live campaign against the stored hash whenever the client signs (`SIGN_DOC`). Any direct mutation of the campaign entity between those two actions will cause F4.

---

### Signing Order

```
1. Client creates campaign (PENDING)
2. Moderator signs first (PENDING → AT_SIGNING)
     ↑ document frozen here
3. Client signs second (AT_SIGNING → WAITING_START)
     ↑ hash re-verified here
```

The client **cannot** sign before the moderator. The moderator can sign again (replacing the signature and re-freezing) while the campaign is still in AT_SIGNING — but the `findByCampaign` + save logic handles this as an update.

---

### Validation Points

| Point | What is checked | Code location |
|-------|----------------|---------------|
| Campaign creation | `@NotBlank`, `@Size`, `@URL`, `@NotNull`, `@DecimalMin/Max` | `CampaignRequest` (Bean Validation) |
| Campaign update | Same constraints (all nullable — partial update) | `UpdateCampaignRequest` |
| Dates | `startDate >= today`, `startDate < endDate` | `ClientService.validateDates()` |
| Moderator signing | Status == PENDING, consentAccepted != false | `ModeratorService.actionCampaign()` |
| Client signing | Status == AT_SIGNING, signature exists, hash matches | `ClientService.actionCampaign()` |
| Client hash confirm | If `documentHash` provided, must equal stored hash | `ClientService.requireDocumentHash()` |
| Signed terms change | Dates cannot change after full signing without new cycle | `ClientService.rejectSignedTermChanges()` |
| Transition guard | All status transitions checked by state machine | `CampaignStatus.canTransitionTo()` |

---

### Debugging Signature-Related Issues

**Checklist when something goes wrong:**

1. **"Campaign not found"** → Wrong campaign ID or wrong client API key (access denied check).

2. **"Campaign must be in PENDING status"** → Campaign is not in PENDING. Check current status via `GET /api/moderator/campaigns/{id}`.

3. **"Campaign is not signed by moderator"** → `campaign_signatures` table has no row for this campaign. Moderator must `SIGN_DOC` first.

4. **"Campaign document hash mismatch"** → The campaign was modified after the moderator signed. Query:
   ```sql
   SELECT document_snapshot, document_hash FROM s412939.campaign_signatures WHERE campaign_id = ?;
   ```
   Recompute the hash from the snapshot and compare with what `CampaignDocumentHashUtil.buildDocumentHash(campaign)` would produce from the live DB row.

5. **"Document hash confirmation mismatch"** → The client submitted a `documentHash` that doesn't match the stored one. Either the client fetched a stale signature, or there is a bug in how the client computes the hash.

6. **"Signed campaign terms cannot be changed"** → `RESUME` action was sent with different dates for a fully-signed campaign. Either send the original dates, or initiate a new signing cycle (update campaign → moderator signs again → client signs again).

7. **"Действие недоступно"** → Invalid status transition. Print current status and check the state machine table above.

8. **PDF generation fails** → Check that `campaign_signatures.document_snapshot` is not null and the signature has a font file `fonts/DejaVuSans.ttf` on the classpath. Check app logs for `RuntimeException("Failed to generate PDF...")`.

9. **Tunnel down / "Failed to fetch" in Swagger UI** → Run `nc -z localhost 18080`; if it fails, restart the tunnel (see F8 above).

---

### API Quick Reference

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/client/campaigns` | client | Create campaign |
| `GET` | `/api/client/campaigns` | client | List own campaigns |
| `GET` | `/api/client/campaigns/{id}` | client | Get campaign |
| `PUT` | `/api/client/campaigns/{id}` | client | Update campaign (PENDING/REJECTED only) |
| `POST` | `/api/client/campaigns/{id}` | client | Action: SIGN_DOC / PAUSE / RESUME |
| `GET` | `/api/client/campaigns/{id}/signature` | client | Get signature details + snapshot |
| `GET` | `/api/client/campaigns/{id}/signature/pdf` | client | Download signing PDF |
| `GET` | `/api/moderator/campaigns/status/{status}` | moderator | List campaigns by status |
| `GET` | `/api/moderator/campaigns/{id}` | moderator | Get campaign |
| `POST` | `/api/moderator/campaigns/{id}` | moderator | Action: SIGN_DOC / REJECT / PAUSE |
| `GET` | `/api/moderator/campaigns/{id}/signature` | moderator | Get signature details |
| `GET` | `/api/moderator/campaigns/{id}/signature/pdf` | moderator | Download signing PDF |
| `POST` | `/api/payment/pay` | client | Top up balance |
| `GET` | `/api/payment/balance` | client | Get balance |

---

### Test Data (seeded by V1 migration)

| Type | Name | API Key | Balance |
|------|------|---------|---------|
| Client | Client One | `client-key-1` | 1000.00 |
| Client | Client Two | `client-key-2` | 500.00 |
| Client | Client Three | `client-key-3` | 50.00 |
| Moderator | Moderator One | `moderator-key-1` | — |
| Moderator | Moderator Two | `moderator-key-2` | — |
