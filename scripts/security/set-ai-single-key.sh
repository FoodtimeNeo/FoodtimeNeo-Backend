#!/usr/bin/env bash
# 填写 / 轮换 AI 单 key（默认 DeepSeek），以 systemd 加密凭据方式落盘。
# 密钥从终端读取：不回显、不进 shell 历史、磁盘只保存主机绑定密文。
#
# 用法：
#   sudo scripts/security/set-ai-single-key.sh              # 自检 -> 写凭据 -> 切 .env -> 重启
#   sudo scripts/security/set-ai-single-key.sh --no-restart # 只准备，稍后手动重启
#   sudo scripts/security/set-ai-single-key.sh --skip-test  # 跳过在线自检（离线/受限网络）
#
# 环境变量覆盖：ENV_FILE / SERVICE / BASE_URL / MODEL / HEALTH_URL / CRED_DIR
# 前置条件与原理见 docs/secrets.md。
set -euo pipefail

usage() {
    sed -n '2,12p' "$0" | sed 's/^# \{0,1\}//'
}

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
DEFAULT_ENV_FILE=""
for candidate in "$SCRIPT_DIR/../../.env" "/root/FoodtimeNeo/FoodtimeNeo-Backend/.env"; do
    if [ -f "$candidate" ]; then DEFAULT_ENV_FILE=$(readlink -f "$candidate"); break; fi
done
ENV_FILE="${ENV_FILE:-$DEFAULT_ENV_FILE}"
SERVICE="${SERVICE:-foodtime-backend}"
CRED_DIR="${CRED_DIR:-/etc/credstore.encrypted}"
CRED_NAME="ai_single_api_key"
CRED_FILE="$CRED_DIR/$CRED_NAME"
BASE_URL="${BASE_URL:-https://api.deepseek.com}"
MODEL="${MODEL:-deepseek-flash}"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8080/actuator/health}"
DO_TEST=1
RESTART=1

for arg in "$@"; do
    case "$arg" in
        --no-restart) RESTART=0 ;;
        --skip-test) DO_TEST=0 ;;
        -h|--help) usage; exit 0 ;;
        *) echo "未知参数：$arg" >&2; exit 2 ;;
    esac
done

[ "$(id -u)" -eq 0 ] || { echo "请用 root 运行（需要写 /etc/credstore.encrypted 并重启服务）" >&2; exit 1; }
command -v systemd-creds >/dev/null || { echo "需要 systemd-creds（systemd >= 250）" >&2; exit 1; }
[ -n "$ENV_FILE" ] || { echo "找不到 .env，请用 ENV_FILE=/path/to/.env 指定" >&2; exit 1; }
[ -f "$ENV_FILE" ] || { echo "找不到 $ENV_FILE" >&2; exit 1; }
if ! systemctl cat "$SERVICE" 2>/dev/null | grep -q "LoadCredentialEncrypted=${CRED_NAME}:"; then
    echo "错误：$SERVICE 未配置 LoadCredentialEncrypted=${CRED_NAME}，应用读不到该密钥。" >&2
    echo "请先按 docs/secrets.md 配置 systemd 单元；或加 --no-restart 仅写凭据。" >&2
    exit 1
fi

KEY=""
read -rsp "AI API key（输入不回显）: " KEY || true
echo
[ -n "$KEY" ] || { echo "key 为空，已取消" >&2; exit 1; }
case "$KEY" in
    *,*) echo "单 key 模式不允许英文逗号" >&2; exit 1 ;;
    *[[:space:]]*) echo "key 不应包含空白字符（检查是否误粘贴换行）" >&2; exit 1 ;;
esac

TMP_BODY=$(mktemp)
TMP_CRED=""
cleanup() {
    rm -f "$TMP_BODY"
    if [ -n "$TMP_CRED" ]; then rm -f "$TMP_CRED"; fi
}
trap cleanup EXIT

if [ "$DO_TEST" -eq 1 ]; then
    echo "自检中：$BASE_URL / $MODEL ..."
    HTTP=$(printf 'header = "Authorization: Bearer %s"\n' "$KEY" | \
        curl -sS --max-time 40 --config - -o "$TMP_BODY" -w '%{http_code}' \
        -H 'Content-Type: application/json' \
        --data-binary "{\"model\":\"$MODEL\",\"messages\":[{\"role\":\"user\",\"content\":\"ping\"}],\"max_tokens\":4}" \
        "$BASE_URL/chat/completions") || HTTP=000
    if [ "$HTTP" != "200" ]; then
        echo "自检失败（HTTP $HTTP），未写入任何配置：" >&2
        head -c 300 "$TMP_BODY" >&2; echo >&2
        exit 1
    fi
    echo "自检通过（文本调用 200）"
fi

install -d -m 0700 "$CRED_DIR"
TMP_CRED=$(mktemp "$CRED_DIR/.${CRED_NAME}.XXXXXX")
printf %s "$KEY" | systemd-creds encrypt --name="$CRED_NAME" --with-key=host - "$TMP_CRED"
chmod 600 "$TMP_CRED"
mv -f "$TMP_CRED" "$CRED_FILE"
TMP_CRED=""
unset KEY
echo "已写入加密凭据：$CRED_FILE（主机绑定，磁盘无明文）"

set_env() {
    local name="$1" value="$2"
    if grep -q "^${name}=" "$ENV_FILE"; then
        sed -i "s|^${name}=.*|${name}=${value}|" "$ENV_FILE"
    else
        printf '%s=%s\n' "$name" "$value" >> "$ENV_FILE"
    fi
}
sed -i 's|^AI_SINGLE_API_KEY=.*|# AI_SINGLE_API_KEY 由 systemd 加密凭据注入（勿在 .env 写明文）|' "$ENV_FILE"
set_env AI_SINGLE_ENABLED true
set_env AI_SINGLE_BASE_URL "$BASE_URL"
set_env AI_SINGLE_MODEL "$MODEL"
echo "已更新 $ENV_FILE：AI_SINGLE_ENABLED=true，base-url=$BASE_URL，model=$MODEL"

if [ "$RESTART" -eq 1 ]; then
    systemctl restart "$SERVICE"
    echo "已重启 $SERVICE，等待健康检查 ..."
    for _ in $(seq 1 30); do
        sleep 2
        if curl -sf "$HEALTH_URL" 2>/dev/null | grep -q '"status":"UP"'; then
            echo "健康检查：UP"
            echo "确认日志：journalctl -u $SERVICE -n 50 --no-pager | grep 'AI single-key mode'"
            exit 0
        fi
    done
    echo "服务未在 60 秒内变为 UP，请检查：journalctl -u $SERVICE -n 80 --no-pager" >&2
    exit 1
fi
echo "未重启（--no-restart）。需要时执行：systemctl restart $SERVICE"
