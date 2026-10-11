#!/bin/sh

set -eu
umask 077

BACKUP_DIR=${BACKUP_DIR:-/backups}
LOG_FILE="$BACKUP_DIR/backup-restore.log"

log() {
  printf '[%s] %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$1" | tee -a "$LOG_FILE"
}

backup_name=${1:-}
if [ -z "$backup_name" ]; then
  log "ERROR: Restore requires a backup filename."
  printf 'Usage: restore.sh database_YYYYMMDDTHHMMSSZ.dump\n' >&2
  exit 2
fi

case "$backup_name" in
  */*)
    log "ERROR: Restore rejected a path; provide a backup filename from /backups."
    exit 2
    ;;
  database_*.dump) ;;
  *)
    log "ERROR: Invalid backup filename '$backup_name'."
    exit 2
    ;;
esac

backup_path="$BACKUP_DIR/$backup_name"
if [ ! -f "$backup_path" ]; then
  log "ERROR: Backup file '$backup_name' does not exist."
  exit 2
fi

if [ "${CONFIRM_RESTORE:-}" != YES ]; then
  log "ERROR: Restore of '$backup_name' was not confirmed; set CONFIRM_RESTORE=YES to proceed."
  exit 2
fi

if [ -z "${PGHOST:-}" ] || [ -z "${PGPORT:-}" ] \
  || [ -z "${PGUSER:-}" ] || [ -z "${PGDATABASE:-}" ]; then
  log "ERROR: PGHOST, PGPORT, PGUSER, and PGDATABASE are required."
  exit 2
fi

log "Starting restore from '$backup_name' into database '$PGDATABASE'; existing objects in the backup will be replaced."
restore_output="$BACKUP_DIR/.restore-output-$$"

if ! pg_restore --list "$backup_path" > "$restore_output" 2>&1; then
  tee -a "$LOG_FILE" < "$restore_output"
  rm -f "$restore_output"
  log "ERROR: Backup '$backup_name' is invalid or unreadable; the database was not changed."
  exit 1
fi

if pg_restore \
  --host="$PGHOST" \
  --port="$PGPORT" \
  --username="$PGUSER" \
  --dbname="$PGDATABASE" \
  --clean \
  --if-exists \
  --exit-on-error \
  --no-owner \
  "$backup_path" > "$restore_output" 2>&1; then
  if [ -s "$restore_output" ]; then
    tee -a "$LOG_FILE" < "$restore_output"
  fi
  rm -f "$restore_output"
  log "Database restore completed successfully from '$backup_name'."
else
  restore_status=$?
  tee -a "$LOG_FILE" < "$restore_output"
  rm -f "$restore_output"
  log "ERROR: Database restore from '$backup_name' failed with exit code $restore_status; the database may be partially restored."
  exit "$restore_status"
fi
