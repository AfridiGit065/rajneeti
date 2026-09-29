#!/usr/bin/env bash
set -u
BASE="http://localhost:8080"
TS=$(date +%s)
U="qa_${TS}"
EM="qa_${TS}@rajneeti.test"
P="qa_pass_01"
echo "===REGISTER==="
RG=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/auth/register" -H "Content-Type: application/json" -d "{\"username\":\"$U\",\"email\":\"$EM\",\"password\":\"$P\"}")
echo "REG_HTTP=$RG"
echo "===LOGIN==="
LG=$(curl -s -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"$EM\",\"password\":\"$P\"}")
LHTTP=$(echo "$LG" | grep -oE '"token"\s*:\s*"[^"]+"' | head -1 | sed -E 's/.*"token"\s*:\s*"([^"]+)"/\1/')
echo "TOKEN_LEN=${#LHTTP}"
echo "===PDF_AUTH==="
if [ -n "$LHTTP" ]; then
  curl -s -o /tmp/rajn_stat.pdf -w "PDF_HTTP=%{http_code}\nPDF_TYPE=%{content_type}\nPDF_SIZE=%{size_download}\n" -H "Authorization: Bearer $LHTTP" "$BASE/api/profile/statistics/pdf"
  echo "PDF_MAGIC=$(head -c 5 /tmp/rajn_stat.pdf 2>/dev/null | tr -d '\0')"
  echo "PDF_HAS_EOF=$(tail -c 12 /tmp/rajn_stat.pdf 2>/dev/null | tr -d '\0' | grep -c '%%EOF' )"
else
  echo "PDF_SKIPPED_NO_TOKEN"
fi
echo "===DB_ROWS==="
docker exec rajneeti-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SELECT username FROM rajneeti.users ORDER BY id DESC LIMIT 1 AND username LIKE \"qa_%\"" 2>/dev/null' 2>/dev/null
echo "===DONE==="
