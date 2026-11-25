#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
./deploy/backup.sh
git pull --ff-only
docker compose -f docker-compose.prod.yml up -d --build
docker image prune -f
docker compose -f docker-compose.prod.yml ps
