#!/usr/bin/env bash
# 旧目录删除前的全库备份（custom 格式，可单表恢复）。
# 用法：DB_URL=jdbc:postgresql://... ./scripts/canteen-refresh/backup-canteen.sh
#   或：PGHOST=.. PGUSER=.. PGDATABASE=.. ./scripts/canteen-refresh/backup-canteen.sh
# 输出：backups/canteen-YYYYmmdd-HHMMSS.dump + 同名 .sha256 + uploads 清单
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
BACKUP_DIR="${BACKUP_DIR:-$REPO_ROOT/backups}"
mkdir -p "$BACKUP_DIR"

STAMP=$(date +%Y%m%d-%H%M%S)
DUMP="$BACKUP_DIR/canteen-$STAMP.dump"

if [ -n "${DB_URL:-}" ]; then
  # jdbc:postgresql://host:port/db?currentSchema=foodtime -> 拆出连接参数
  JDBC="${DB_URL#jdbc:postgresql://}"
  HOSTPORT="${JDBC%%/*}"; DBPATH="${JDBC#*/}"
  export PGHOST="${HOSTPORT%%:*}" PGPORT="${HOSTPORT#*:}" PGDATABASE="${DBPATH%%\?*}"
fi
: "${PGHOST:?PGHOST 或 DB_URL 必填}" "${PGUSER:?PGUSER 或 DB_URL 必填}" "${PGDATABASE:?PGDATABASE 或 DB_URL 必填}"

pg_dump -Fc -f "$DUMP"
test -s "$DUMP" || { echo "备份文件为空，拒绝继续"; exit 1; }
sha256sum "$DUMP" | tee "$DUMP.sha256"

# uploads 快照清单（只记清单不拷贝，恢复时对照； uploads 本体由卷快照/备份另行保障）
UPLOAD_DIR="${UPLOAD_DIR:-/app/uploads}"
if [ -d "$UPLOAD_DIR" ]; then
  find "$UPLOAD_DIR" -type f | sort > "$BACKUP_DIR/canteen-$STAMP.uploads.txt"
  echo "uploads 文件数：$(wc -l < "$BACKUP_DIR/canteen-$STAMP.uploads.txt")"
fi
echo "备份完成：$DUMP"
echo "回滚：pg_restore -c -d \"\$PGDATABASE\" $DUMP（确认库名后执行）"
