#!/bin/sh

set -eu
umask 077

BACKUP_DIR=${BACKUP_DIR:-/backups}
LOG_FILE="$BACKUP_DIR/backup-restore.log"
BACKUP_TIME_UTC=${BACKUP_TIME_UTC:-19:00}

log() {
  printf '[%s] %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$1" | tee -a "$LOG_FILE"
}

if [ -z "${PGHOST:-}" ] || [ -z "${PGPORT:-}" ] \
  || [ -z "${PGUSER:-}" ] || [ -z "${PGDATABASE:-}" ]; then
  log "ERROR: PGHOST, PGPORT, PGUSER, and PGDATABASE are required."
  exit 2
fi

case "$BACKUP_TIME_UTC" in
  [0-9][0-9]:[0-9][0-9]) ;;
  *)
    log "ERROR: Invalid BACKUP_TIME_UTC '$BACKUP_TIME_UTC' (expected HH:MM in UTC)."
    exit 2
    ;;
esac

backup_hour=${BACKUP_TIME_UTC%%:*}
backup_minute=${BACKUP_TIME_UTC#*:}
case "$backup_hour" in
  0[0-9]) ;;
  1[0-9]) ;;
  2[0-3]) ;;
  *)
    log "ERROR: Invalid BACKUP_TIME_UTC '$BACKUP_TIME_UTC' (expected HH:MM in UTC)."
    exit 2
    ;;
esac
case "$backup_minute" in
  [0-5][0-9]) ;;
  *)
    log "ERROR: Invalid BACKUP_TIME_UTC '$BACKUP_TIME_UTC' (expected HH:MM in UTC)."
    exit 2
    ;;
esac

while :; do
  now=$(date -u +%s)
  next_run=$(date -u -d "today $BACKUP_TIME_UTC" +%s)
  if [ "$next_run" -lt "$now" ]; then
    next_run=$(date -u -d "tomorrow $BACKUP_TIME_UTC" +%s)
  fi

  wait_seconds=$((next_run - now))
  log "Next database backup is scheduled in ${wait_seconds}s."
  sleep "$wait_seconds"

  backup_name="database_$(date -u '+%Y%m%dT%H%M%SZ').dump"
  backup_path="$BACKUP_DIR/$backup_name"
  temporary_path="$BACKUP_DIR/.${backup_name}.tmp.$$"
  output_path="$temporary_path.log"
  log "Creating database backup $backup_name."

  if pg_dump \
    --host="$PGHOST" \
    --port="$PGPORT" \
    --username="$PGUSER" \
    --dbname="$PGDATABASE" \
    --format=custom \
    --no-owner \
    --file="$temporary_path" > "$output_path" 2>&1; then
    if [ -s "$output_path" ]; then
      tee -a "$LOG_FILE" < "$output_path"
    fi
    rm -f "$output_path"
  else
    backup_status=$?
    tee -a "$LOG_FILE" < "$output_path"
    rm -f "$output_path"
    rm -f "$temporary_path"
    log "ERROR: Database backup failed with exit code $backup_status; existing backups were preserved."
    exit "$backup_status"
  fi

  if ! mv "$temporary_path" "$backup_path"; then
    rm -f "$temporary_path"
    log "ERROR: Could not finalize database backup; existing backups were preserved."
    exit 1
  fi

  log "Database backup completed: $backup_name."

  find "$BACKUP_DIR" -maxdepth 1 -type f -name 'database_*.dump' -printf '%f\n' \
    | sort -r \
    | tail -n +8 \
    | while IFS= read -r old_backup; do
        [ -n "$old_backup" ] || continue
        rm -f -- "$BACKUP_DIR/$old_backup"
        log "Removed expired backup $old_backup."
      done
done
