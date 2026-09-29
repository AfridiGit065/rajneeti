#!/usr/bin/env bash
set -u
BASE="http://localhost:8080"
U="rajn_qa_$(date +%s)"
P="qa_pass_$(openssl rand -hex 8)"
echo "===AUTH==="
BODY=$(printf '{"username":"%s","password":"%s"}' "$U" "$P")
REG=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/auth/register" -H "Content-Type: application/json" -d "$BODY")
RCODE=$(echo "$REG" | tail -1)
echo "REGISTER_HTTP=$RCODE"
LOG=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" -d "$BODY")
LCODE=$(echo "$LOG" | tail -1)
echo "LOGIN_HTTP=$LCODE"
LBODY=$(echo "$LOG" | sed '$d')
TOK=$(echo "$LBODY" | grep -oE '"token"[[:space:]]*:[[:space:]]*"[^"]+"' | sed -E 's/.*"token"[[:space:]]*:[[:space:]]*"([^"]+)"/\1/' | head -1)
if [ -n "$TOK" ]; then echo "TOKEN_LEN=${#TOK}"; else echo "TOKEN_NONE"; fi
echo "===PDF==="
if [ -n "$TOK" ]; then
  curl -s -o /tmp/rajn_stat.pdf -w "PDF_HTTP=%{http_code}\nPDF_TYPE=%{content_type}\nPDF_SIZE=%{size_download}\n" \
    -H "Authorization: Bearer $TOK" "$BASE/api/profile/statistics/pdf"
  echo -n "PDF_MAGIC="; head -c 5 /tmp/rajn_stat.pdf 2>/dev/null | tr -d '\0' | cat -v
  echo ""
fi
echo "===UNAUTH_PDF==="
curl -s -o /tmp/rajn_noauth.pdf -w "UNAUTH_HTTP=%{http_code}\n" "$BASE/api/profile/statistics/pdf"
echo "===DBTABLES==="
docker exec rajneeti-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SHOW TABLES;" 2>/dev/null' 2>/dev/null | tr '\n' ' '
echo ""
echo "===WINNER_ROWS==="
docker exec rajneeti-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SELECT COUNT(*) FROM rajneeti.match_history" 2>/dev/null' 2>/dev/null
echo "===DONE==="
