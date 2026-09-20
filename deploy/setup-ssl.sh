#!/usr/bin/env bash
# ==============================================================================
# RAJNEETI - Free SSL (HTTPS + WSS) with Let's Encrypt (certbot) + Nginx proxy
#
# Usage:  sudo ./deploy/setup-ssl.sh game.yourdomain.com your-email@example.com
#
# Requires:
#   - A domain name pointing (A record) to this server's public IP
#   - Ports 80 + 8080 reachable from the internet
#
# What it does:
#   - Installs Nginx + certbot
#   - Obtains a Let's Encrypt certificate for your domain
#   - Configures Nginx as a reverse proxy:
#       https://your.domain  ->  http://localhost:3000  (frontend)
#       https://your.domain/api, /ws  ->  http://localhost:8080  (backend)
#   - Auto-renews certificates every 60 days via systemd timer
# ==============================================================================

set -euo pipefail

if [ "$EUID" -ne 0 ]; then
  echo "[-] Please run as root: sudo ./deploy/setup-ssl.sh web.rajneeti.app you@email.com"
  exit 1
fi

DOMAIN="${1:-}"
EMAIL="${2:-}"
if [ -z "$DOMAIN" ] || [ -z "$EMAIL" ]; then
  echo "Usage: sudo ./deploy/setup-ssl.sh your.domain.com your-email@example.com"
  exit 1
fi

echo "🔐 Setting up HTTPS + WSS for $DOMAIN ..."

echo "[1/5] Installing Nginx and certbot..."
apt-get update -y
apt-get install -y nginx certbot python3-certbot-nginx

echo "[2/5] Creating Nginx reverse proxy config for $DOMAIN..."
cat > /etc/nginx/sites-available/rajneeti <<EOF
server {
    listen 80;
    server_name ${DOMAIN};

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /ws {
        proxy_pass http://127.0.0.1:8080/ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
    }

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF

ln -sf /etc/nginx/sites-available/rajneeti /etc/nginx/sites-enabled/rajneeti
rm -f /etc/nginx/sites-enabled/default
nginx -t

echo "[3/5] Obtaining Let's Encrypt certificate..."
certbot --nginx -d "$DOMAIN" --non-interactive --agree-tos -m "$EMAIL" --redirect

echo "[4/5] Reloading Nginx..."
systemctl reload nginx

echo "[5/5] Installing auto-renewal timer..."
if ! systemctl list-timers | grep -q certbot; then
  systemctl enable certbot.timer >/dev/null 2>&1 || true
fi
systemctl start certbot.timer 2>/dev/null || true

echo ""
echo "============================================================"
echo "🎉 HTTPS is LIVE!"
echo "============================================================"
echo "  👉 Game:   https://${DOMAIN}"
echo ""
echo "IMPORTANT: after switching to HTTPS, update your .env so"
echo "browsers allow API/WS calls from the new origin:"
echo ""
echo "  nano .env"
echo "    CORS_ALLOWED_ORIGINS=https://${DOMAIN}"
echo "    WS_ALLOWED_ORIGINS=https://${DOMAIN}"
echo "    NEXT_PUBLIC_API_URL=https://${DOMAIN}/api"
echo "    NEXT_PUBLIC_WS_URL=wss://${DOMAIN}/ws"
echo ""
echo "  then:  docker compose up -d --build"
echo "============================================================"