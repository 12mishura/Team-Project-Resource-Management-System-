#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
BACKUP_DIR="${BACKUP_DIR:-$HOME/tasktracker-backups}"
KEEP_DAYS="${KEEP_DAYS:-14}"
COMPOSE=(docker compose -f docker-compose.prod.yml)
if [ -n "${ENV_FILE:-}" ]; then
  COMPOSE+=(--env-file "$ENV_FILE")
fi

mkdir -p "$BACKUP_DIR"
file="$BACKUP_DIR/tasktracker-$(date -u +%Y%m%d-%H%M%S).sql.gz"
trap 'rm -f "$file"' ERR

"${COMPOSE[@]}" exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -d tasktracker --no-owner --clean --if-exists' | gzip > "$file"
trap - ERR

find "$BACKUP_DIR" -name 'tasktracker-*.sql.gz' -mtime +"$KEEP_DAYS" -delete

if [ -n "${BACKUP_S3_BUCKET:-}" ]; then
  aws s3 cp "$file" "s3://$BACKUP_S3_BUCKET/" --only-show-errors
fi

echo "Backup saved to $file"
