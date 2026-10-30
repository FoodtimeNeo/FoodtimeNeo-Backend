#!/usr/bin/env bash
# 本机生产更新（systemd + Docker PostgreSQL）：食堂两批数据合并更新。
# 顺序：前置检查 -> 双目录备份 -> 构建新 jar -> 两批删除 -> 重启（Flyway V24–V28）
#       -> 铺 v25/v28 图片 -> 合并对账。
# 用法：
#   bash scripts/canteen-refresh/apply-canteen-all-local.sh --check   # 只检查，不改动
#   bash scripts/canteen-refresh/apply-canteen-all-local.sh           # 正式执行
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
CONTAINER="${CONTAINER:-foodtime-neo-postgres-1}"
PGUSER="${PGUSER:-foodtime}"
DBNAME="${DBNAME:-foodtime_neo}"
SERVICE="${SERVICE:-foodtime-backend}"
UPLOAD_DIR="${UPLOAD_DIR:-$REPO_ROOT/uploads}"
SEED_USER="${SEED_USER:-39d46f72-3ea4-57b9-a25f-8d3a6779e84d}"
BACKUP_EXT="${BACKUP_EXT:-/var/backups/foodtime}"
BACKUP_REPO="${BACKUP_REPO:-$REPO_ROOT/backups}"
TS="$(date +%Y%m%d-%H%M%S)"
EXT="$BACKUP_EXT/pre-data-update-$TS"
REP="$BACKUP_REPO/pre-data-update-$TS"
CHECK_ONLY=0
if [ "${1:-}" = "--check" ]; then CHECK_ONLY=1; fi

db_psql() {
    docker exec -i "$CONTAINER" psql -U "$PGUSER" -d "$DBNAME" -v ON_ERROR_STOP=1 "$@"
}
db_scalar() {
    db_psql -Atc "$1"
}
fail() {
    echo "错误：$*" >&2
    echo "未自动回滚。若已执行删除/重启，请用本次备份回滚：" >&2
    echo "  systemctl stop $SERVICE" >&2
    echo "  docker exec -i $CONTAINER pg_restore -U $PGUSER -d $DBNAME -c < $EXT/foodtime_neo.dump" >&2
    echo "  systemctl start $SERVICE" >&2
    exit 1
}

echo "=== 前置检查 $TS ==="
[ "$(id -u)" -eq 0 ] || fail "请用 root 运行"
systemctl is-active --quiet "$SERVICE" || fail "$SERVICE 未运行"
[ "$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null)" = "true" ] || fail "容器 $CONTAINER 未运行"
[ -d "$UPLOAD_DIR" ] && [ -w "$UPLOAD_DIR" ] || fail "uploads 目录不可写：$UPLOAD_DIR"
[ "$(db_scalar "SELECT count(*) FROM foodtime.users WHERE id='$SEED_USER';")" = "1" ] || fail "种子用户不存在：$SEED_USER"
[ "$(find "$REPO_ROOT/seed-images/v25" -name '*.jpg' | wc -l)" -eq 32 ] || fail "seed-images/v25 不是 32 张"
[ "$(find "$REPO_ROOT/seed-images/v28" -name '*.jpg' | wc -l)" -eq 43 ] || fail "seed-images/v28 不是 43 张"
[ "$(db_scalar "SELECT count(*) FROM foodtime.dining_halls WHERE id IN ('74ed9519-f3e3-5ce8-a89c-a353e2892edd','315086d7-f339-5d26-a030-b3d7ffe7cde1');")" = "2" ] \
    || fail "旧明湖/学活厅不存在，可能已更新过"
MIG=$(db_scalar "SELECT max(version::int) FROM foodtime.flyway_schema_history WHERE success;")
[ "$MIG" = "23" ] || fail "期望迁移版本 23，实际 $MIG（可能已更新过）"
echo "前置检查通过：迁移 V$MIG，种子用户/图片齐全"
if [ "$CHECK_ONLY" = "1" ]; then
    echo "--check 完成，未做任何改动。"
    exit 0
fi

echo "=== 1/7 双目录备份 ==="
umask 077
mkdir -p "$BACKUP_EXT" "$EXT" "$REP"
chmod 700 "$BACKUP_EXT" "$EXT" "$REP"
docker exec "$CONTAINER" pg_dump -U "$PGUSER" -d "$DBNAME" -Fc > "$EXT/foodtime_neo.dump"
[ -s "$EXT/foodtime_neo.dump" ] || fail "数据库备份为空"
docker exec -i "$CONTAINER" pg_restore -l < "$EXT/foodtime_neo.dump" > "$EXT/restore-list.txt" \
    || fail "备份文件校验失败"
tar -czf "$EXT/uploads.tar.gz" -C "$REPO_ROOT" uploads
tar -czf "$EXT/config.tar.gz" \
    -C "$REPO_ROOT" .env \
    -C /etc/systemd/system "$SERVICE.service" \
    -C /usr/local/bin foodtime-backend-start.sh \
    -C /www/server/panel/vhost/nginx foodtime-web.conf kfcvivo50.ddns.net.conf kfcvivo50.ddns.net.443.conf
if [ -f "$REPO_ROOT/target/foodtime-neo-backend.jar" ]; then
    cp -a "$REPO_ROOT/target/foodtime-neo-backend.jar" "$EXT/"
fi
{
    echo "created=$(date -Is)"
    echo "host=$(hostname)"
    echo "backend_commit=$(git -C "$REPO_ROOT" rev-parse HEAD)"
    echo "backend_subject=$(git -C "$REPO_ROOT" log -1 --format=%s)"
    echo "db_migration_before_update=$MIG"
    db_scalar "SELECT 'users='||count(*) FROM foodtime.users UNION ALL SELECT 'dining_halls='||count(*) FROM foodtime.dining_halls UNION ALL SELECT 'stalls='||count(*) FROM foodtime.stalls UNION ALL SELECT 'dishes='||count(*) FROM foodtime.dishes UNION ALL SELECT 'dish_images='||count(*) FROM foodtime.dish_images UNION ALL SELECT 'dish_reviews='||count(*) FROM foodtime.dish_reviews UNION ALL SELECT 'community_posts='||count(*) FROM foodtime.community_posts;"
} > "$EXT/manifest.txt"
(cd "$EXT" && sha256sum * > sha256sums.txt)
cp -a "$EXT/." "$REP/"
(cd "$REP" && sha256sum -c sha256sums.txt >/dev/null) || fail "项目副本校验失败"
chmod -R go-rwx "$EXT" "$REP"
echo "备份：$EXT"
echo "副本：$REP"

echo "=== 2/7 构建新 jar（含 V24–V28） ==="
cd "$REPO_ROOT"
if [ -f target/foodtime-neo-backend.jar ]; then
    mv target/foodtime-neo-backend.jar "target/foodtime-neo-backend.jar.pre-canteen-$TS"
fi
bash ./mvnw -B -ntp -DskipTests package > "/tmp/opencode/canteen-build-$TS.log" 2>&1 \
    || fail "构建失败，详见 /tmp/opencode/canteen-build-$TS.log（数据库未删除）"
echo "构建完成"

echo "=== 3/7 删除两批旧目录（先删后重启） ==="
db_psql -f - < "$SCRIPT_DIR/delete-old-catalog.sql"
db_psql -f - < "$SCRIPT_DIR/delete-old-catalog-2.sql"

echo "=== 4/7 重启应用，Flyway 应用 V24–V28 ==="
systemctl restart "$SERVICE"
UP=0
for _ in $(seq 1 60); do
    sleep 5
    if curl -sf "http://127.0.0.1:8080/actuator/health" | grep -q '"status":"UP"'; then UP=1; break; fi
done
[ "$UP" = "1" ] || fail "应用 5 分钟内未健康"
MIG_AFTER=$(db_scalar "SELECT max(version::int) FROM foodtime.flyway_schema_history WHERE success;")
[ "$MIG_AFTER" = "28" ] || fail "期望迁移 28，实际 $MIG_AFTER"
echo "迁移版本：$MIG_AFTER，健康检查通过"

echo "=== 5/7 铺图（v25 + v28） ==="
place_seed() {
    local src="$1" expect="$2" placed=0 skipped=0 f uid
    for f in "$src"/*.jpg; do
        uid=$(basename "$f" .jpg)
        if [ -s "$UPLOAD_DIR/$uid.img" ]; then
            [ "$(sha256sum "$f" | cut -d' ' -f1)" = "$(sha256sum "$UPLOAD_DIR/$uid.img" | cut -d' ' -f1)" ] \
                || fail "已存在图片内容不一致：$uid"
            skipped=$((skipped+1))
            continue
        fi
        cp "$f" "$UPLOAD_DIR/$uid.img"
        printf 'owner=%s\ncontentType=image/jpeg\n' "$SEED_USER" > "$UPLOAD_DIR/$uid.properties"
        placed=$((placed+1))
    done
    echo "  $(basename "$src"): placed=$placed skipped=$skipped expect=$expect"
    [ $((placed+skipped)) -eq "$expect" ] || fail "图片数量不符：$(basename "$src")"
}
place_seed "$REPO_ROOT/seed-images/v25" 32
place_seed "$REPO_ROOT/seed-images/v28" 43

echo "=== 6/7 合并对账 ==="
VERIFY_LOG="/tmp/opencode/canteen-verify-$TS.log"
db_psql -f - < "$SCRIPT_DIR/verify-canteen-all.sql" | tee "$VERIFY_LOG"
if grep -q 'FAIL' "$VERIFY_LOG"; then
    fail "对账存在 FAIL，详见 $VERIFY_LOG"
fi

echo "=== 7/7 媒体引用检查 ==="
MEDIA_LIST=$(mktemp)
{
    db_scalar "SELECT regexp_replace(image_url,'^.*/','') FROM foodtime.stall_images;"
    db_scalar "SELECT regexp_replace(cover_image_url,'^.*/','') FROM foodtime.stalls WHERE cover_image_url IS NOT NULL;"
} | sort -u > "$MEDIA_LIST"
MISSING=0
while IFS= read -r uid; do
    [ -n "$uid" ] || continue
    [ -s "$UPLOAD_DIR/$uid.img" ] || { echo "缺少图片：$uid"; MISSING=$((MISSING+1)); }
done < "$MEDIA_LIST"
rm -f "$MEDIA_LIST"
[ "$MISSING" -eq 0 ] || fail "有 $MISSING 个媒体引用找不到文件"

echo "=== 更新完成 ==="
echo "备份：$EXT"
echo "副本：$REP"
echo "对账：$VERIFY_LOG"
echo "回滚：systemctl stop $SERVICE; docker exec -i $CONTAINER pg_restore -U $PGUSER -d $DBNAME -c < $EXT/foodtime_neo.dump; systemctl start $SERVICE"
