#!/usr/bin/env bash
# cURL smoke test for the Darija Translator REST service.
# Usage: ./test-api.sh [baseUrl] [username] [password]
#   default: http://localhost:8080/darija/api  ali  darija2026

BASE="${1:-http://localhost:8080/darija/api}"
USER_NAME="${2:-ali}"
PASS="${3:-darija2026}"

line() { printf '\n=== %s ===\n' "$1"; }

line "1. Health check (public)"
curl -s -w '\nHTTP %{http_code}\n' "$BASE/health"

line "2. POST translate, Arabic script (expect 200)"
curl -s -w '\nHTTP %{http_code}\n' -u "$USER_NAME:$PASS" \
     -H 'Content-Type: application/json' \
     -d '{"text":"Hello, how are you? I would like a coffee please.","script":"arabic"}' \
     "$BASE/translator/translate"

line "3. POST translate, Latin script (expect 200)"
curl -s -w '\nHTTP %{http_code}\n' -u "$USER_NAME:$PASS" \
     -H 'Content-Type: application/json' \
     -d '{"text":"Bonjour, où est la gare ?","script":"latin"}' \
     "$BASE/translator/translate"

line "4. GET translate with query parameter (expect 200)"
curl -s -w '\nHTTP %{http_code}\n' -u "$USER_NAME:$PASS" -G \
     --data-urlencode 'text=Good morning, my friend' \
     "$BASE/translator/translate"

line "5. No credentials (expect 401 + WWW-Authenticate header)"
curl -s -D - -o /dev/null \
     -H 'Content-Type: application/json' -d '{"text":"Hello"}' \
     "$BASE/translator/translate" | grep -i -E '^(HTTP|WWW-Authenticate)'

line "6. Wrong password (expect 401)"
curl -s -o /dev/null -w 'HTTP %{http_code}\n' -u "$USER_NAME:wrong" \
     -H 'Content-Type: application/json' -d '{"text":"Hello"}' \
     "$BASE/translator/translate"

line "7. Empty text (expect 400)"
curl -s -w '\nHTTP %{http_code}\n' -u "$USER_NAME:$PASS" \
     -H 'Content-Type: application/json' -d '{"text":"   "}' \
     "$BASE/translator/translate"
