# RAJNEETI — Cloud Deployment Guide

Host RAJNEETI on any cloud VM in ~15 minutes so friends can play **from any PC**.

> Prefer local/LAN? See the docker-compose path: `docker compose up -d --build` then
> open `http://<YOUR_LAN_IP>:3000` (add `<YOUR_LAN_IP>:3000` + `:8080` to CORS vars).

---

## Option A — Oracle Cloud Free Tier (recommended, $0/month)

Oracle's **Always Free** tier gives you 2 VMs (1 GB RAM each) forever.

### 1. Create the VM (5 min)
1. Go to https://cloud.oracle.com → **Sign up** (Free) → verify email + card.
2. Console → menu ≡ → **Compute → Instances → Create instance**.
3. **Name:** `rajneeti`
4. **Image:** Ubuntu **24.04** (shape section → *Change image* → Ubuntu 24.04).
5. **Shape:** click *Change shape* → check **"Specialty and legacy"** →
   pick **VM.Standard.E2.1.Micro** (Always Free, AMD, 1 GB RAM).
   > Prefer ARM? Choose *"Arm"* → **VM.Standard.A1.Flex** with 1 OCPU / 6 GB RAM.
6. **Networking:** default VCN is fine.
7. **Add SSH keys:** *Generate a key pair* → **Download private key** → keep safe.
8. **Boot volume:** change to **Balanced** (or leave default).

### 2. Open firewall ports (2 min)
1. Instance page → **Attached VNICs** → click VNIC → **Subnet** → click subnet.
2. **Security Lists** → *Default Security List* → **Add Ingress Rules** for:
   - `22/tcp` (SSH, usually already open)
   - `3000/tcp` (game)   — source `0.0.0.0/0`
   - `8080/tcp` (API/WS) — source `0.0.0.0/0`

### 3. SSH in + deploy (5 min)
```bash
chmod 400 ojVM_rajneeti_key.pem
ssh -i ojVM_rajneeti_key.pem ubuntu@<PUBLIC_IP>

# once inside the VM:
git clone https://github.com/AfridiGit065/rajneeti.git
cd rajneeti
chmod +x deploy/*.sh
sudo ./deploy/setup.sh
```
The script will:
- create a 4 GB swapfile (prevents OOM on the free VM),
- install Docker Engine + Compose, configure UFW,
- auto-fill your public URLs into `.env`,
- pause so you can set secrets (`JWT_SECRET`, `DB_PASSWORD`, AI keys),
- build & launch all 3 containers.

### 4. Play!
- Game: `http://<PUBLIC_IP>:3000`
- Health: `http://<PUBLIC_IP>:8080/api/health` → `{"status":"UP",...}`

Send `http://<PUBLIC_IP>:3000` to friends — done.

---

## Option B — Any VPS (DigitalOcean / Hetzner / Azure B1s)

Same one-liner on any Ubuntu 22.04/24.04 VM (2 GB RAM recommended):
```bash
git clone https://github.com/AfridiGit065/rajneeti.git
cd rajneeti
chmod +x deploy/*
sudo ./deploy/setup.sh
```
Only the firewall differs per provider (open 22, 3000, 8080).

---

## HTTPS + WSS (free SSL) — optional

Point a domain (A record → VM IP), then:
```bash
sudo ./deploy/setup-ssl.sh game.yourdomain.com you@email.com
```
This installs Nginx + Let's Encrypt and reverse-proxies:
`https://your.domain` → frontend, `/api` and `/ws` → backend.
It also prints the `.env` CORS/WS updates needed for HTTPS (`wss://`).

---

## Updating after code changes
```bash
cd rajneeti
./deploy/update.sh        # git pull + rebuild + restart
```
Or manually: `docker compose pull && docker compose build && docker compose up -d`

---

## Useful commands
```bash
docker compose ps                         # container status
docker compose logs -f app                # backend logs
docker compose logs -f frontend           # frontend logs
docker compose down && docker compose up -d    # full restart
docker system prune -af                   # free disk space
```

---

## Troubleshooting
| Symptom | Fix |
|---|---|
| Browser shows API/CORS error | Confirm `.env` `CORS_ALLOWED_ORIGINS` + `WS_ALLOWED_ORIGINS` include `http://<IP>:3000`, then `docker compose up -d --build` |
| WebSocket won't connect | `NEXT_PUBLIC_WS_URL` must be `ws://<IP>:8080/ws` (or `wss://` with SSL) — rebuild frontend |
| Containers crash-loop (OOM) | Check `docker compose logs app`. Swap script ran? At least 2 GB total RAM+swap needed for Java build |
| Ports unreachable from outside | Cloud firewall (security list) AND UFW both must allow 3000/8080 |
| Re-deploy after secret change | `nano .env` → edit → `docker compose up -d --build` |