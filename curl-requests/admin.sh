#!/usr/bin/env bash
# Admin operations: create manager, list/mutate privileges.
set -euo pipefail
BASE="${BASE:-http://localhost:8080}"
: "${ADMIN_TOKEN:?Run auth.sh and export ADMIN_TOKEN first}"

echo "== Create manager =="
curl -sS -X POST "$BASE/api/admin/managers" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"username":"bob-mod","password":"bob-pass","name":"Bob Moderator"}'
echo

echo "== List privileges =="
curl -sS -X GET "$BASE/api/admin/privileges" \
    -H "Authorization: Bearer $ADMIN_TOKEN"
echo

echo "== List role -> privileges =="
curl -sS -X GET "$BASE/api/admin/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN"
echo

echo "== Revoke CAMPAIGN_DELETE_ANY from MANAGER =="
curl -sS -X POST "$BASE/api/admin/roles/MANAGER/privileges" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"privilegeCode":"CAMPAIGN_DELETE_ANY","operation":"REVOKE"}'
echo

echo "== Grant CAMPAIGN_DELETE_ANY back to MANAGER =="
curl -sS -X POST "$BASE/api/admin/roles/MANAGER/privileges" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"privilegeCode":"CAMPAIGN_DELETE_ANY","operation":"GRANT"}'
echo
