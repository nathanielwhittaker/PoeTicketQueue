# Deploying PoeTicketQueue

A small self-hosted deploy: one Docker container behind a Cloudflare Tunnel, with
access limited to an allow-list of emails via Cloudflare Access. No inbound ports are
opened on the host — the tunnel connector dials out to Cloudflare.

> **Note:** all state (groups, queues, Discord config) is in memory. Restarting or
> updating the container wipes everything; members re-create their groups afterward.

## Overview

```
buddy's browser ──https──> Cloudflare (Access email gate) ──tunnel──> cloudflared ──> app:8080
```

- `app` — the Spring Boot + Vue image, published to GHCR by the Release workflow.
- `cloudflared` — the tunnel connector (compose `tunnel` profile).

---

## 1. One-time: cut a release

The **Release** workflow (`.github/workflows/release.yml`) builds and pushes the image
to GHCR on any `v*` tag:

```bash
git tag v0.1.0
git push origin v0.1.0
```

This publishes `ghcr.io/nathanielwhittaker/poeticketqueue:{0.1.0, 0.1, latest}`.

**Make the package pullable from the deploy box** (simplest: public — the image holds no
secrets):
Repo → **Packages** → `poeticketqueue` → **Package settings** → **Change visibility** →
Public. (If you keep it private, run `docker login ghcr.io` on the box with a PAT that
has `read:packages`.)

---

## 2. One-time: create the Cloudflare Tunnel

In the **Zero Trust** dashboard (dash.cloudflare.com → Zero Trust):

1. **Networks → Tunnels → Create a tunnel** → type **Cloudflared** → name it (e.g.
   `poe-ticket-queue`).
2. On the **Install connector** screen, copy the token — it's the long string after
   `--token` in the shown command. You do **not** run that command; compose runs the
   connector for you.
3. **Public Hostnames → Add a public hostname:**
   - **Subdomain/Domain:** pick the hostname buddies will use, e.g. `poe.example.com`.
   - **Service:** `HTTP` → `app:8080`  ← the compose service name, reachable on the
     connector's network.
4. Save.

---

## 3. One-time: restrict access (Cloudflare Access)

Still in Zero Trust:

1. **Access → Applications → Add an application → Self-hosted.**
2. **Application domain:** the same hostname (`poe.example.com`).
3. **Add a policy:**
   - **Action:** Allow
   - **Include → Emails:** list your son's buddies' emails (or **Emails ending in** a
     domain).
4. Login method: the built-in **One-time PIN** is enough — buddies enter their email,
   get a code, done. No accounts to manage.

Now only allow-listed emails can reach the app; everyone else is blocked at Cloudflare.

---

## 4. Deploy on the box

```bash
cp .env.example .env
# edit .env: set APP_VERSION=v0.1.0's tag (e.g. 0.1.0) and paste TUNNEL_TOKEN
docker compose --profile tunnel pull
docker compose --profile tunnel up -d
```

Check it:

```bash
docker compose ps                     # app healthy, cloudflared running
docker compose logs -f cloudflared    # should show a registered connection
```

Visit the public hostname — you should hit the Cloudflare Access email prompt, then the
app.

## Updating to a new release

```bash
# bump APP_VERSION in .env to the new tag, then:
docker compose --profile tunnel pull
docker compose --profile tunnel up -d
```

(Reminder: this restarts the app and clears all in-memory state.)

---

## Running locally without the tunnel

Omit the profile to run just the app on `127.0.0.1:8080` (no tunnel, no token needed):

```bash
docker compose up -d
```

Build from local source instead of pulling a release:

```bash
docker compose -f docker-compose.yml -f docker-compose.build.yml up -d --build
```
