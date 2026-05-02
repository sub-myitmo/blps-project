#!/usr/bin/env bash
# Payment flow: deposit, balance, denial paths.
set -euo pipefail
BASE="${BASE:-http://localhost:8080}"
: "${CLIENT_TOKEN:?Run auth.sh and export CLIENT_TOKEN first}"

echo "== Deposit 500 =="
curl -sS -X POST "$BASE/api/payment/pay" \
    -H "Authorization: Bearer $CLIENT_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"amount":500.00,"description":"Bank card top-up"}'
echo

echo "== Get balance =="
curl -sS -X GET "$BASE/api/payment/balance" \
    -H "Authorization: Bearer $CLIENT_TOKEN"
echo

echo "== Deposit without token (expect 401) =="
curl -sS -o /dev/null -w "HTTP %{http_code}\n" -X POST "$BASE/api/payment/pay" \
    -H "Content-Type: application/json" \
    -d '{"amount":1.00,"description":"unauth"}'

echo "== Deposit with admin token (expect 403, no PAYMENT_TOPUP) =="
if [ -n "${ADMIN_TOKEN:-}" ]; then
  curl -sS -o /dev/null -w "HTTP %{http_code}\n" -X POST "$BASE/api/payment/pay" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"amount":1.00,"description":"admin denied"}'
else
  echo "(skip — ADMIN_TOKEN not set)"
fi
