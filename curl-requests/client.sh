#!/usr/bin/env bash
# Client happy-path. Requires CLIENT_TOKEN exported (run auth.sh first).
set -euo pipefail
BASE="${BASE:-http://localhost:8080}"
: "${CLIENT_TOKEN:?Run auth.sh and export CLIENT_TOKEN first}"
AUTH=(-H "Authorization: Bearer $CLIENT_TOKEN" -H "Content-Type: application/json")

echo "== Create campaign (T+1 .. T+30) =="
curl -sS -X POST "$BASE/api/client/campaigns" "${AUTH[@]}" -d '{
    "name":"Новогодняя распродажа",
    "content":"Скидки до 70% на все товары!",
    "targetUrl":"https://shop.ru/new-year",
    "dailyBudget":150.00,
    "startDate":"'$(date -v+1d +%Y-%m-%d 2>/dev/null || date -d "+1 day" +%Y-%m-%d)'",
    "endDate":"'$(date -v+30d +%Y-%m-%d 2>/dev/null || date -d "+30 days" +%Y-%m-%d)'"
}'
echo

echo "== List own campaigns =="
curl -sS -X GET "$BASE/api/client/campaigns" -H "Authorization: Bearer $CLIENT_TOKEN"
echo

echo "== View campaign 1 =="
curl -sS -X GET "$BASE/api/client/campaigns/1" -H "Authorization: Bearer $CLIENT_TOKEN"
echo

echo "== Pause campaign 2 =="
curl -sS -X POST "$BASE/api/client/campaigns/2" "${AUTH[@]}" -d '{"action":"PAUSE"}'
echo

echo "== Resume campaign 2 =="
curl -sS -X POST "$BASE/api/client/campaigns/2" "${AUTH[@]}" -d '{
    "action":"RESUME",
    "startDate":"'$(date -v+5d +%Y-%m-%d 2>/dev/null || date -d "+5 days" +%Y-%m-%d)'",
    "endDate":"'$(date -v+90d +%Y-%m-%d 2>/dev/null || date -d "+90 days" +%Y-%m-%d)'"
}'
echo

echo "== Sign campaign 1 (after moderator SIGN_DOC) =="
curl -sS -X POST "$BASE/api/client/campaigns/1" "${AUTH[@]}" -d '{"action":"SIGN_DOC","consentAccepted":true}'
echo

echo "== Get signature details =="
curl -sS -X GET "$BASE/api/client/campaigns/1/signature" -H "Authorization: Bearer $CLIENT_TOKEN"
echo

echo "== Invalid: start date in past (expect 4xx) =="
curl -sS -o /dev/null -w "HTTP %{http_code}\n" -X POST "$BASE/api/client/campaigns" "${AUTH[@]}" -d '{
    "name":"Bad","content":"x","targetUrl":"https://x","dailyBudget":1,"startDate":"2020-01-01"
}'
