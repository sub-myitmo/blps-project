#!/usr/bin/env bash
# Negative-path checks: 401 for no/invalid token, 403 for missing privileges.
set -euo pipefail
BASE="${BASE:-http://localhost:8080}"
: "${CLIENT_TOKEN:?Run auth.sh and export CLIENT_TOKEN first}"
: "${ADMIN_TOKEN:?Run auth.sh and export ADMIN_TOKEN first}"

echo "== Client hitting /api/admin/privileges (expect 403) =="
curl -sS -o /dev/null -w "HTTP %{http_code}\n" -X GET "$BASE/api/admin/privileges" \
    -H "Authorization: Bearer $CLIENT_TOKEN"

echo "== Admin hitting /api/client/campaigns (expect 403, no CAMPAIGN_VIEW_OWN) =="
curl -sS -o /dev/null -w "HTTP %{http_code}\n" -X GET "$BASE/api/client/campaigns" \
    -H "Authorization: Bearer $ADMIN_TOKEN"

echo "== No token on protected endpoint (expect 401) =="
curl -sS -o /dev/null -w "HTTP %{http_code}\n" -X GET "$BASE/api/client/campaigns"

echo "== Tampered token (expect 401) =="
curl -sS -o /dev/null -w "HTTP %{http_code}\n" -X GET "$BASE/api/client/campaigns" \
    -H "Authorization: Bearer not.a.real.token"

echo "== Manager hitting /api/payment/pay (expect 403) =="
MANAGER_LOGIN=$(curl -sS -X POST "$BASE/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"manager-key-1","password":"password"}' || true)
MGR=$(echo "$MANAGER_LOGIN" | python3 -c "import json,sys
try:
    d=json.load(sys.stdin); print(d.get('token',''))
except Exception:
    print('')" 2>/dev/null)
if [ -n "$MGR" ]; then
  curl -sS -o /dev/null -w "HTTP %{http_code}\n" -X POST "$BASE/api/payment/pay" \
      -H "Authorization: Bearer $MGR" \
      -H "Content-Type: application/json" \
      -d '{"amount":1,"description":"x"}'
else
  echo "(skip — no legacy manager seeded)"
fi
