#!/usr/bin/env bash
set -u
BASE="http://localhost:8080"
TS=$(date +%s)
U="qa_${TS}"
EM="${U}@rajneeti.test"
P="qa_pass_01"
echo "===REGISTER_FLAG==="
R=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/auth/register" -H "Content-Type: application/json" -d "{\"username\":\"$U\",\"email\":\"$EM\",\"password\":\"$P\"}")
echo "REG_HTTP=$(echo "$R" | tail -1)"
echo "===REGISTER_BODY==="
echo "$R" | sed '$d' | cut -c1-220
echo "===LOGIN_FLAG==="
L=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"$EM\",\"password\":\"$P\"}")
echo "LOG_HTTP=$(echo "$L" | tail -1)"
LB=$(echo "$L" | sed '$d')
echo "===LOGIN_BODY_SHAPE==="
echo "$LB" | grep -oE '"[a-zA-Z]+"|true|false|[0-9]+' | tr '\n' ' '; echo ""
TOK=$(echo "$LB" | grep -oE '"token"\s*:\s*"[^"]+"' | sed -E 's/.*"token"\s*:\s*"([^"]+)"/\1/')
echo "TOKEN_LEN=${#TOK}"
echo "===PDF_AUTHED==="
if [ -n "$TOK" ]; then
  curl -s -o /tmp/rajn_s25.pdf -w "PDF_HTTP=%{http_code} PDF_TYPE=%{content_type} PDF_BYTES=%{size_download}\n" -H "Authorization: Bearer $TOK" "$BASE/api/profile/statistics/pdf"
fi
echo "PDF_MAGIC=$(head -c 5 /tmp/rajn_s25.pdf 2>/dev/null | tr -d '\0')"
echo "PDF_EOF=$(tail -c 10 /tmp/rajn_s25.pdf 2>/dev/null | tr -d '\0' | grep -c '%%EOF')"
echo "===PDF_UNAUTH==="
curl -s -o /dev/null -w "PDF_UNAUTH_HTTP=%{http_code}\n" "$BASE/api/profile/statistics/pdf"
echo "===COMPOSE_PS==="
docker compose ps
echo "===HEALTH==="
curl -s -o /dev/null -w "HEALTH_HTTP=%{http_code}\n" "$BASE/api/health"
echo "===DB_CONS==="
docker exec rajneeti-mysql mysqladmin -uroot -p"${MYSQL_ROOT_PASSWORD}" ping 2>/dev/null | sed 's/^/MYSQL_/'
echo "===DB_USERS_TABLES==="
docker exec rajneeti-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -N -e "USE rajneeti; SHOW TABLES;" 2>/dev/null | grep -Ei 'user|player|account' | head -12 | tr '\n' ' '; echo ""
echo "===LOGS_ERRORS==="
docker compose logs rajneeti-app --since=6m 2>&1 | grep -iE 'Exception|ERROR|Caused by|Communications|SQL' | head -8
echo "===DONE25==="
