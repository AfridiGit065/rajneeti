#!/usr/bin/env bash
set -u
BASE="http://localhost:8080"
echo "===REGFIELDS==="
REGCLASS=$(docker exec rajneeti-app sh -c 'unzip -l /app/app.jar 2>/dev/null | grep -oE "com/rajneeti/dto/[A-Za-z]+Register[A-Za-z]*Request.class" | head -3' 2>/dev/null)
echo "$REGCLASS"
if [ -n "$REGCLASS" ]; then
  C1=$(echo "$REGCLASS" | head -1)
  docker exec rajneeti-app sh -c "unzip -p /app/app.jar $C1 > /tmp/regreq.class 2>/dev/null && javap -p /tmp/regreq.class" 2>/dev/null | grep -E '\b(private|public) ' | sed 's/^  //' | head -20
fi
echo "===AUTHCTRL==="
docker exec rajneeti-app sh -c 'unzip -l /app/app.jar 2>/dev/null | grep -oE "com/rajneeti/controller/[A-Za-z]+Auth[A-Za-z]*Controller.class" | head -3' 2>/dev/null
echo "===USERSCOUNT==="
docker exec rajneeti-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SELECT COUNT(*) FROM rajneeti.users" 2>/dev/null' 2>/dev/null || echo USER_COUNT_UNAVAILABLE
echo "===TABLES_REAL==="
docker exec rajneeti-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SHOW TABLES FROM rajneeti" 2>/dev/null' 2>/dev/null | sort | tr "\n" " "
echo ""
echo "===DONE==="
