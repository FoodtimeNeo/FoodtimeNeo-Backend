#!/usr/bin/env bash
# 食堂数据更新一键串行：备份 -> 删旧 -> 重启应用（Flyway 自动到 V25）-> 铺图 -> 对账。
# 任一步失败即停，按输出的回滚指引处理，不要跳步。
# 前置：分支已合入并在服务器 git pull；镜像已重新构建（含 V24/V25）。
# 用法（Backend 根目录）：
#   DB_URL='jdbc:postgresql://localhost:5432/foodtime_neo?currentSchema=foodtime' \
#   PGUSER=foodtime PGPASSWORD=xxx bash scripts/canteen-refresh/apply-one-click.sh
# 可选：SERVICE=foodtime-backend COMPOSE_FILE=compose.prod.yml UPLOAD_DIR=/app/uploads APP_PORT=8080
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
cd "$REPO_ROOT"

COMPOSE_FILE="${COMPOSE_FILE:-compose.prod.yml}"
UPLOAD_DIR="${UPLOAD_DIR:-/app/uploads}"
APP_PORT="${APP_PORT:-8080}"
SEED_USER="${SEED_USER:-39d46f72-3ea4-57b9-a25f-8d3a6779e84d}"
export BACKUP_DIR="${BACKUP_DIR:-$REPO_ROOT/backups}"

step() { echo "=== [$1] $2 ==="; }

step 1 "备份全库"
bash "$SCRIPT_DIR/backup-canteen.sh"

step 2 "删除旧目录（单事务，删不干净自动回滚）"
psql "$DB_URL" -v ON_ERROR_STOP=1 -f "$SCRIPT_DIR/delete-old-catalog.sql"

step 3 "重启应用，Flyway 自动应用 V24/V25"
docker compose -f "$COMPOSE_FILE" up -d app
for i in $(seq 1 60); do
  if curl -sf -m 3 "http://127.0.0.1:$APP_PORT/actuator/health" >/dev/null; then
    echo "应用健康检查通过（${i}0s 内）"
    break
  fi
  if [ "$i" = 60 ]; then echo "应用 10 分钟未健康，停在这里，先排查再继续"; exit 1; fi
  sleep 10
done
echo "确认迁移版本（expect V25）："
psql "$DB_URL" -tAc "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1;"

step 4 "铺图片（seed-images/v25/*.jpg -> uploads/<uuid>.img + .properties，已存在的不覆盖）"
python3 - "$SCRIPT_DIR/../../seed-images" "$UPLOAD_DIR" "$SEED_USER" <<'EOF'
import os, sys
seed_root, dst, owner = sys.argv[1], sys.argv[2], sys.argv[3]
os.makedirs(dst, exist_ok=True)
placed, skipped = 0, 0
src = os.path.join(seed_root, "v25")
for name in sorted(os.listdir(src)):
    if not name.endswith(".jpg"):
        continue
    uid = name[:-4]
    img = os.path.join(dst, uid + ".img")
    prop = os.path.join(dst, uid + ".properties")
    if os.path.exists(img) and os.path.getsize(img) > 0:
        skipped += 1
        continue
    with open(os.path.join(src, name), "rb") as f, open(img, "wb") as o:
        o.write(f.read())
    with open(prop, "w", encoding="ascii") as f:
        f.write("owner=" + owner + "\ncontentType=image/jpeg\n")
    placed += 1
print("placed=%d skipped=%d" % (placed, skipped))
EOF

step 5 "对账"
psql "$DB_URL" -v ON_ERROR_STOP=1 -f "$SCRIPT_DIR/verify-canteen.sql"

echo "全部完成。13 个 0 元菜品待食堂核价后改价（见对账输出）。"
echo "回滚：BACKUP_DIR 下最近的 .dump 用 pg_restore -c 恢复，然后重启应用。"
