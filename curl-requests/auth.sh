#!/usr/bin/env bash
# Auth flow: register client, login client/manager/admin, capture JWT.
set -euo pipefail
BASE="${BASE:-http://localhost:8080}"

echo "== Register new client =="
REG=$(curl -sS -X POST "$BASE/api/auth/register" \
    -H "Content-Type: application/json" \
    -d '{"username":"alice","password":"alice-pass","name":"Alice Inc."}')
echo "$REG"
CLIENT_TOKEN=$(echo "$REG" | python3 -c "import json,sys; print(json.load(sys.stdin)['token'])")
echo "CLIENT_TOKEN=$CLIENT_TOKEN"

echo "== Login as admin =="
ADMIN_LOGIN=$(curl -sS -X POST "$BASE/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin"}')
echo "$ADMIN_LOGIN"
ADMIN_TOKEN=$(echo "$ADMIN_LOGIN" | python3 -c "import json,sys; print(json.load(sys.stdin)['token'])")
echo "ADMIN_TOKEN=$ADMIN_TOKEN"

echo "== Login as legacy manager (manager-key-1 / password) =="
MANAGER_LOGIN=$(curl -sS -X POST "$BASE/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"manager-key-1","password":"password"}' || true)
echo "$MANAGER_LOGIN"

echo "Export tokens:"
echo "  export CLIENT_TOKEN=$CLIENT_TOKEN"
echo "  export ADMIN_TOKEN=$ADMIN_TOKEN"
