#!/usr/bin/env bash
# Moderator/manager flow. Requires MANAGER_TOKEN.
set -euo pipefail
BASE="${BASE:-http://localhost:8080}"
: "${MANAGER_TOKEN:?Login as manager and export MANAGER_TOKEN first}"
AUTH=(-H "Authorization: Bearer $MANAGER_TOKEN" -H "Content-Type: application/json")

echo "== List by status PENDING =="
curl -sS -X GET "$BASE/api/moderator/campaigns/status/PENDING" -H "Authorization: Bearer $MANAGER_TOKEN"
echo

echo "== Sign campaign 1 (PENDING -> AT_SIGNING) =="
curl -sS -X POST "$BASE/api/moderator/campaigns/1" "${AUTH[@]}" -d '{"action":"SIGN_DOC","consentAccepted":true,"comment":"Проверено"}'
echo

echo "== Reject campaign 2 =="
curl -sS -X POST "$BASE/api/moderator/campaigns/2" "${AUTH[@]}" -d '{"action":"REJECT","comment":"Контент запрещён"}'
echo

echo "== Pause campaign 3 =="
curl -sS -X POST "$BASE/api/moderator/campaigns/3" "${AUTH[@]}" -d '{"action":"PAUSE","comment":"Нарушение"}'
echo

echo "== Add moderation comment to campaign 1 =="
curl -sS -X POST "$BASE/api/moderator/campaigns/1/comments" "${AUTH[@]}" -d '{"comment":"Дополнительная заметка"}'
echo

echo "== List by status REJECTED =="
curl -sS -X GET "$BASE/api/moderator/campaigns/status/REJECTED" -H "Authorization: Bearer $MANAGER_TOKEN"
echo

echo "== Get campaign 1 =="
curl -sS -X GET "$BASE/api/moderator/campaigns/1" -H "Authorization: Bearer $MANAGER_TOKEN"
echo

echo "== Invalid action (expect 4xx) =="
curl -sS -o /dev/null -w "HTTP %{http_code}\n" -X POST "$BASE/api/moderator/campaigns/1" "${AUTH[@]}" -d '{"action":"INVALID","comment":"x"}'
