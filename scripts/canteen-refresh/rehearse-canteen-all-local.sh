#!/usr/bin/env bash
# 彩排（不接触生产）：从备份恢复临时库 -> 两批删除 -> 应用 V24–V28 ->
# 铺图到临时目录 -> 合并对账 -> 对比未受影响数据。
# 用法：bash scripts/canteen-refresh/rehearse-canteen-all-local.sh
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
DUMP="${DUMP:-/var/backups/foodtime/pre-data-update-20261030-194941/foodtime_neo.dump}"
CONTAINER="${CONTAINER:-foodtime-neo-postgres-1}"
PGUSER="${PGUSER:-foodtime}"
SEED_USER="${SEED_USER:-39d46f72-3ea4-57b9-a25f-8d3a6779e84d}"
TS="$(date +%Y%m%d-%H%M%S)"
TESTDB="foodtime_rehearsal_$(date +%s)"
TMP_UPLOADS="/tmp/opencode/rehearsal-uploads-$TS"
REPORT="/tmp/opencode/canteen-rehearsal-$TS.log"
KEEP_TMP="${KEEP_TMP:-0}"

mkdir -p /tmp/opencode
exec > >(tee -a "$REPORT") 2>&1

db_psql() {
    docker exec -i "$CONTAINER" psql -U "$PGUSER" -d "$TESTDB" -v ON_ERROR_STOP=1 "$@"
}
db_scalar() {
    db_psql -Atc "$1"
}

cleanup() {
    local code=$?
    if [ "${DB_CREATED:-0}" = "1" ]; then
        docker exec "$CONTAINER" psql -U "$PGUSER" -d postgres \
            -c "DROP DATABASE IF EXISTS $TESTDB;" >/dev/null 2>&1 || true
    fi
    if [ "$KEEP_TMP" = "0" ] && [ -d "$TMP_UPLOADS" ]; then
        rm -rf "$TMP_UPLOADS"
    fi
    echo "报告：$REPORT"
    [ "$code" -eq 0 ] || echo "彩排失败，临时库已清理"
}
trap cleanup EXIT

[ -s "$DUMP" ] || { echo "找不到备份：$DUMP" >&2; exit 1; }
[ -d "$REPO_ROOT/seed-images/v25" ] && [ -d "$REPO_ROOT/seed-images/v28" ] || {
    echo "缺少 seed-images/v25 或 v28" >&2; exit 1; }
[ -d "$REPO_ROOT/scripts/canteen-refresh" ] || exit 1

echo "=== 彩排开始 $TS ==="
echo "备份：$DUMP"
echo "临时库：$TESTDB"
echo "临时图片目录：$TMP_UPLOADS"

echo "[1/7] 恢复临时库"
docker exec "$CONTAINER" psql -U "$PGUSER" -d postgres -v ON_ERROR_STOP=1 \
    -c "CREATE DATABASE $TESTDB;"
DB_CREATED=1
docker exec -i "$CONTAINER" pg_restore -U "$PGUSER" -d "$TESTDB" \
    --no-owner --no-privileges < "$DUMP"

echo "[2/7] 记录更新前快照"
PRE_USERS=$(db_scalar "SELECT count(*) FROM foodtime.users;")
PRE_POSTS=$(db_scalar "SELECT count(*) FROM foodtime.community_posts;")
PRE_REVIEWS=$(db_scalar "SELECT count(*) FROM foodtime.dish_reviews;")
PRE_LIUYUAN=$(db_scalar "SELECT md5(string_agg(d.id::text||'|'||d.name||'|'||d.price::text, ',' ORDER BY d.id)) FROM foodtime.dishes d JOIN foodtime.stalls s ON s.id=d.stall_id JOIN foodtime.dining_halls h ON h.id=s.dining_hall_id WHERE h.name='留园食堂';")
echo "users=$PRE_USERS community_posts=$PRE_POSTS reviews=$PRE_REVIEWS liuyuan_md5=$PRE_LIUYUAN"

echo "[3/7] 执行两批旧目录删除（生产顺序：先删后重启）"
db_psql -f - < "$SCRIPT_DIR/delete-old-catalog.sql"
db_psql -f - < "$SCRIPT_DIR/delete-old-catalog-2.sql"

echo "[4/7] 应用 V24–V28"
for f in V24__add_stall_images.sql V25__refresh_minghu_dongqu_catalog.sql \
         V26__notices_messages.sql V27__login_notice_markdown.sql \
         V28__refresh_xuehuo_xuesi_dongqu.sql; do
    echo "  -> $f"
    # -1：与 Flyway 一样按单事务执行（V28 的 TEMP TABLE ... ON COMMIT DROP 依赖此语义）
    db_psql -q -1 -f - < "$REPO_ROOT/src/main/resources/db/migration/$f"
done

echo "[5/7] 铺图到临时目录"
mkdir -p "$TMP_UPLOADS"
place_seed() {
    local src="$1" expect="$2" placed=0 skipped=0
    local f uid
    for f in "$src"/*.jpg; do
        uid=$(basename "$f" .jpg)
        if [ -s "$TMP_UPLOADS/$uid.img" ]; then
            skipped=$((skipped+1))
            continue
        fi
        cp "$f" "$TMP_UPLOADS/$uid.img"
        printf 'owner=%s\ncontentType=image/jpeg\n' "$SEED_USER" > "$TMP_UPLOADS/$uid.properties"
        placed=$((placed+1))
    done
    echo "  $(basename "$src"): placed=$placed skipped=$skipped expect=$expect"
    if [ "$placed" -ne "$expect" ] && [ "$skipped" -ne "$expect" ]; then
        echo "  图片数量不符" >&2
        return 1
    fi
    for f in "$src"/*.jpg; do
        uid=$(basename "$f" .jpg)
        [ "$(sha256sum "$f" | cut -d' ' -f1)" = "$(sha256sum "$TMP_UPLOADS/$uid.img" | cut -d' ' -f1)" ] || {
            echo "  图片内容不一致：$uid" >&2; return 1; }
    done
}
place_seed "$REPO_ROOT/seed-images/v25" 32
place_seed "$REPO_ROOT/seed-images/v28" 43

echo "[6/7] 合并对账"
VERIFY_LOG="$REPORT.verify"
db_psql -f - < "$SCRIPT_DIR/verify-canteen-all.sql" | tee "$VERIFY_LOG"
if grep -q 'FAIL' "$VERIFY_LOG"; then
    echo "对账存在 FAIL" >&2
    exit 1
fi

echo "[7/7] 未受影响数据与图片引用核对"
POST_USERS=$(db_scalar "SELECT count(*) FROM foodtime.users;")
POST_POSTS=$(db_scalar "SELECT count(*) FROM foodtime.community_posts;")
POST_REVIEWS=$(db_scalar "SELECT count(*) FROM foodtime.dish_reviews;")
POST_LIUYUAN=$(db_scalar "SELECT md5(string_agg(d.id::text||'|'||d.name||'|'||d.price::text, ',' ORDER BY d.id)) FROM foodtime.dishes d JOIN foodtime.stalls s ON s.id=d.stall_id JOIN foodtime.dining_halls h ON h.id=s.dining_hall_id WHERE h.name='留园食堂';")
echo "users $PRE_USERS -> $POST_USERS"
echo "community_posts $PRE_POSTS -> $POST_POSTS"
echo "reviews $PRE_REVIEWS -> $POST_REVIEWS"
echo "liuyuan_md5 $PRE_LIUYUAN -> $POST_LIUYUAN"
[ "$PRE_USERS" = "$POST_USERS" ] || { echo "用户数变化" >&2; exit 1; }
[ "$PRE_POSTS" = "$POST_POSTS" ] || { echo "社区帖数变化" >&2; exit 1; }
[ "$PRE_LIUYUAN" = "$POST_LIUYUAN" ] || { echo "留园菜品内容变化" >&2; exit 1; }

MEDIA_LIST=$(mktemp)
{
    db_scalar "SELECT regexp_replace(image_url,'^.*/','') FROM foodtime.stall_images;"
    db_scalar "SELECT regexp_replace(cover_image_url,'^.*/','') FROM foodtime.stalls WHERE cover_image_url IS NOT NULL;"
} | sort -u > "$MEDIA_LIST"
MISSING=0
while IFS= read -r uid; do
    [ -n "$uid" ] || continue
    if [ ! -s "$TMP_UPLOADS/$uid.img" ] && [ ! -s "$REPO_ROOT/uploads/$uid.img" ]; then
        echo "  缺少图片文件: $uid"
        MISSING=$((MISSING+1))
    fi
done < "$MEDIA_LIST"
rm -f "$MEDIA_LIST"
echo "媒体引用缺失：$MISSING"
[ "$MISSING" -eq 0 ] || { echo "有媒体引用找不到文件" >&2; exit 1; }

echo "=== 彩排全部通过 ==="
