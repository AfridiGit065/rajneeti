#!/usr/bin/env bash
# ==============================================================================
# RAJNEETI - Update script: pull latest code, rebuild, restart containers.
# Usage:  ./deploy/update.sh    (or:  sudo ./deploy/update.sh)
# ==============================================================================

set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
echo "🔁 Updating RAJNEETI at $REPO_DIR"

if ! command -v docker >/dev/null 2>&1; then
  echo "[-] Docker not found. Run sudo ./deploy/setup.sh first."
  exit 1
fi

cd "$REPO_DIR"

echo "[1/3] Pulling latest code..."
if [ -d .git ]; then
  git config --global --add safe.directory "$REPO_DIR" 2>/dev/null || true
  git pull --ff-only || { echo "[-] git pull failed. Commit/stash local changes first."; exit 1; }
else
  echo "[!] Not a git repo; using existing files."
fi

echo "[2/3] Rebuilding images..."
docker compose pull
docker compose build

echo "[3/3] Restarting containers..."
docker compose up -d

echo "[+] Done. Verify with: docker compose ps"