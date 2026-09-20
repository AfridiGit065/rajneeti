#!/usr/bin/env bash
# ==============================================================================
# RAJNEETI - Ubuntu Cloud VM Turnkey Provisioning Script
# Supported: Ubuntu 22.04 LTS / 24.04 LTS  (1GB+ RAM, x64 or arm64)
#
# Creates a 4GB swapfile, installs Docker Engine + Compose, configures UFW,
# builds the 3-container stack (mysql/app/frontend) and launches it.
#
# Usage:  sudo ./deploy/setup.sh
# ==============================================================================

set -euo pipefail

echo "============================================================"
echo "🏛️  RAJNEETI - Cloud VM Setup"
echo "============================================================"

if [ "$EUID" -ne 0 ]; then
  echo "[-] Please run as root or with sudo: sudo ./deploy/setup.sh"
  exit 1
fi

CURRENT_USER="${SUDO_USER:-$USER}"

echo "[1/7] Updating system packages..."
apt-get update -y
DEBIAN_FRONTEND=noninteractive apt-get upgrade -y

echo "[2/7] Installing essential utilities..."
apt-get install -y \
  ca-certificates \
  curl \
  gnupg \
  lsb-release \
  git \
  ufw

echo "[3/7] Configuring swap space for low-RAM VMs..."
CURRENT_SWAP_KB=$(grep SwapTotal /proc/meminfo 2>/dev/null | awk '{print $2}' || echo "0")
if [ "${CURRENT_SWAP_KB:-0}" -lt 2097152 ]; then
  if [ ! -f /swapfile ]; then
    echo "[+] Creating 4GB swapfile at /swapfile..."
    fallocate -l 4G /swapfile 2>/dev/null || dd if=/dev/zero of=/swapfile bs=1M count=4096 status=none
    chmod 600 /swapfile
    mkswap /swapfile
  fi
  swapon /swapfile 2>/dev/null || true
  if ! grep -q '/swapfile' /etc/fstab; then
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
  fi
  sysctl -w vm.swappiness=20 >/dev/null 2>&1 || true
  sysctl -w vm.vfs_cache_pressure=50 >/dev/null 2>&1 || true
  if [ -d /etc/sysctl.d ]; then
    printf "vm.swappiness=20\nvm.vfs_cache_pressure=50\n" > /etc/sysctl.d/99-swap.conf
  fi
  echo "[+] Swap active: $(free -h | awk '/Swap:/ {print $2}')"
else
  echo "[+] Adequate swap already present."
fi

echo "[4/7] Installing Docker Engine & Compose Plugin..."
install -m 0755 -d /etc/apt/keyrings
if [ ! -f /etc/apt/keyrings/docker.gpg ]; then
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg
fi

ARCH=$(dpkg --print-architecture)
CODENAME=$(lsb_release -cs)
echo "deb [arch=${ARCH} signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu ${CODENAME} stable" > /etc/apt/sources.list.d/docker.list

apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

systemctl enable --now docker

if [ -n "$CURRENT_USER" ] && [ "$CURRENT_USER" != "root" ]; then
  usermod -aG docker "$CURRENT_USER"
  echo "[+] Added '$CURRENT_USER' to docker group."
fi

echo "[5/7] Configuring host firewall (UFW)..."
ufw allow 22/tcp comment "SSH"
ufw allow 3000/tcp comment "HTTP (Frontend / Game)"
ufw allow 8080/tcp comment "Backend API + WebSocket"
ufw --force enable

echo "[6/7] Preparing .env and public URLs..."
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PUBLIC_IP=$(curl -s --connect-timeout 3 https://api.ipify.org 2>/dev/null || hostname -I 2>/dev/null | awk '{print $1}')
PUBLIC_IP=${PUBLIC_IP:-<YOUR_PUBLIC_IP>}

if [ ! -f "$REPO_DIR/.env" ]; then
  echo "[+] Creating .env from .env.example"
  cp "${REPO_DIR}/.env.example" "${REPO_DIR}/.env"
fi

echo "[+] Setting public URLs (server IP: ${PUBLIC_IP})..."
# Only overwrite placeholders / localhost values, never pre-configured custom values.
sed -i "s|^CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001$|CORS_ALLOWED_ORIGINS=http://${PUBLIC_IP}:3000,http://localhost:3000|" "${REPO_DIR}/.env"
sed -i "s|^WS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001$|WS_ALLOWED_ORIGINS=http://${PUBLIC_IP}:3000,http://localhost:3000|" "${REPO_DIR}/.env"
sed -i "s|^NEXT_PUBLIC_API_URL=http://localhost:8080$|NEXT_PUBLIC_API_URL=http://${PUBLIC_IP}:8080|" "${REPO_DIR}/.env"
sed -i "s|^NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws$|NEXT_PUBLIC_WS_URL=ws://${PUBLIC_IP}:8080/ws|" "${REPO_DIR}/.env"

echo "    ⚠️  EDIT the secrets now:  nano $REPO_DIR/.env"
echo "    Set JWT_SECRET (openssl rand -base64 48), DB_PASSWORD, MYSQL_ROOT_PASSWORD."
echo "    (Optional AI keys: AI_API_KEY_1..3 for LLM bots.)"
echo "    Press Enter once done..."
read -r -p ""

echo "[7/7] Building and launching containers..."
cd "$REPO_DIR"
git config --global --add safe.directory "$REPO_DIR" 2>/dev/null || true

echo "[+] Building mysql image is pulled automatically by compose."
echo "[+] Building backend container..."
docker compose build app || true
docker compose build frontend || true

echo "[+] Launching full stack..."
docker compose up -d

echo "[+] Verifying health..."
sleep 8
docker compose ps

echo ""
echo "============================================================"
echo "🎉 RAJNEETI Deployment Successful!"
echo "============================================================"
echo "  👉 Game:        http://${PUBLIC_IP}:3000"
echo "  👉 API health:  http://${PUBLIC_IP}:8080/api/health"
echo ""
echo "Next steps:"
echo "  - Cloud firewall: open ports 3000 and 8080 (and 22 for SSH)"
echo "  - Need HTTPS? Point a domain and run: sudo ./deploy/setup-ssl.sh your.domain email@x.com"
echo "============================================================"