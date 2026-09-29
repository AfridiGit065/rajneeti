#!/usr/bin/env bash
set -u
BASE="http://localhost:8080"
TS=$(date +%s)
U="qa_fa_${TS}"
EM="${U}@rajneeti.test"
P="qa_pass_01"
echo "===REGISTER==="
R=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/auth/register" -H "Content-Type: application/json" -d "{\"username\":\"$U\",\"email\":\"$EM\",\"password\":\"$P\"}")
echo "REG_HTTP=$(echo "$R" | tail -1)"
B=$(echo "$R" | sed '$d')
echo "REG_ID_PRESENT=$(echo "$B" | grep -c '"id"')"
echo "===LOGIN==="
L=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"$EM\",\"password\":\"$P\"}")
echo "LOG_HTTP=$(echo "$L" | tail -1)"
LB=$(echo "$L" | sed '$d')
TOK=$(echo "$LB" | grep -oE '"accessToken"\s*:\s*"[^"]+"' | sed -E 's/.*"accessToken"\s*:\s*"([^"]+)"/\1/' | head -1)
echo "TOKEN_LEN=${#TOK}"
echo "===PDF_AUTHED==="
if [ -n "$TOK" ]; then
  curl -s -o /tmp/rajn_stat_fa.pdf -w "PDF_HTTP=%{http_code}\nPDF_TYPE=%{content_type}\nPDF_BYTES=%{size_download}\n" -H "Authorization: Bearer $TOK" "$BASE/api/profile/statistics/pdf"
  echo "PDF_HEAD=$(head -c 5 /tmp/rajn_stat_fa.pdf 2>/dev/null | tr -d '\0' | cat -v)"
  echo "PDF_TAIL=$(tail -c 12 /tmp/rajn_stat_fa.pdf 2>/dev/null | tr -d '\0' | cat -v)"
  echo "PDF_HAS_TITLE=$(strings /tmp/rajn_stat_fa.pdf 2>/dev/null | grep -ci 'rajneeti')"
fi
echo "===PDF_UNAUTH==="
curl -s -o /dev/null -w "PDF_UNAUTH_HTTP=%{http_code}\n" "$BASE/api/profile/statistics/pdf"
echo "===DB_ROW==="
docker exec rajneeti-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "USE rajneeti; SELECT id, username, rating, total_matches FROM users WHERE username LIKE \"qa_fa_%\" ORDER BY id DESC LIMIT 1;" 2>/dev/null' 2>/dev/null
echo "===DONE==="
