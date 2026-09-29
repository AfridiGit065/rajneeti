#!/usr/bin/env bash
set -u
BASE="http://localhost:8080"
TS=$(date +%s)
U="qa_fa_${TS}"
EM="${U}@rajneeti.test"
P="qa_pass_01"
R=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/auth/register" -H "Content-Type: application/json" -d "{\"username\":\"$U\",\"email\":\"$EM\",\"password\":\"$P\"}")
echo "REG_HTTP=$(echo "$R" | tail -1)"
L=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"$EM\",\"password\":\"$P\"}")
echo "LOG_HTTP=$(echo "$L" | tail -1)"
LB=$(echo "$L" | sed '$d')
TOK=$(echo "$LB" | grep -oE '"accessToken"\s*:\s*"[^"]+"' | sed -E 's/.*"accessToken"\s*:\s*"([^"]+)"/\1/' | head -1)
echo "TOKEN_LEN=${#TOK}"
if [ -n "$TOK" ]; then
  curl -s -o /tmp/rajn_fa.pdf -w "PDF_HTTP=%{http_code} PDF_TYPE=%{content_type} PDF_BYTES=%{size_download}\n" \
       -H "Authorization: Bearer $TOK" "$BASE/api/profile/statistics/pdf"
  echo "PDF_MAGIC=$(head -c 5 /tmp/rajn_fa.pdf 2>/dev/null | tr -d '\0' | cat -v)"
  echo "PDF_EOF_COUNT=$(grep -c '%%EOF' /tmp/rajn_fa.pdf 2>/dev/null || true)"
  echo "PDF_PRODUCER=$(grep -aoE '/Producer \(OpenPDF[^)]*\)' /tmp/rajn_fa.pdf 2>/dev/null | head -1)"
  echo "PDF_TITLE=$(grep -aoE '/Title \(Rajneeti[^)]*\)' /tmp/rajn_fa.pdf 2>/dev/null | head -1)"
fi
echo "===UNAUTH==="
curl -s -o /dev/null -w "PDF_UNAUTH_HTTP=%{http_code}\n" "$BASE/api/profile/statistics/pdf"
echo "===DB==="
docker exec rajneeti-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SELECT COUNT(*) AS stat_users_total FROM rajneeti.users" 2>/dev/null' 2>/dev/null
echo "===DB_QA_MOST_RECENT==="
docker exec rajneeti-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "USE rajneeti; SELECT username, email FROM users WHERE username LIKE \"qa_fa_%\" ORDER BY id DESC LIMIT 1;" 2>/dev/null' 2>/dev/null
echo "===DONE==="
