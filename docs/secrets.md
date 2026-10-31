# 密钥安全存储（AI API Key）

AI 密钥不进仓库、不以明文落盘：部署时用 **systemd 加密凭据**（`systemd-creds`，主机绑定）
保存密文，服务启动时解密到内存并注入环境变量。

## 原理

1. `systemd-creds encrypt --with-key=host` 用本机主机密钥加密
   （`/var/lib/systemd/credential.secret`，root 0400，只在本机可解密）。
2. systemd 单元用 `LoadCredentialEncrypted=` 在服务启动时解密到
   `$CREDENTIALS_DIRECTORY`（tmpfs，仅服务用户可读）。
3. 启动包装脚本把凭据读入 `AI_SINGLE_API_KEY` / `AI_TRIAGE_API_KEY` 后 `exec java`。
4. `.env` 中不得出现 `AI_SINGLE_API_KEY` / `AI_TRIAGE_API_KEY` 明文。

加密只保护静态存储；拥有 root 的主机管理员仍可在运行时读取进程环境或解密凭据，
这是任何本机方案都无法避免的边界。

## 一次性服务器配置

要求 systemd >= 250（Debian 12 为 252）。

`/etc/systemd/system/foodtime-backend.service`：

```ini
[Service]
WorkingDirectory=/root/FoodtimeNeo/FoodtimeNeo-Backend
LoadCredentialEncrypted=ai_single_api_key:/etc/credstore.encrypted/ai_single_api_key
LoadCredentialEncrypted=ai_triage_api_key:/etc/credstore.encrypted/ai_triage_api_key
ExecStart=/usr/local/bin/foodtime-backend-start.sh
```

`/usr/local/bin/foodtime-backend-start.sh`（root 755，脚本内无密钥）：

```sh
#!/bin/sh
set -a
if [ -n "$CREDENTIALS_DIRECTORY" ] && [ -f "$CREDENTIALS_DIRECTORY/ai_single_api_key" ]; then
    AI_SINGLE_API_KEY=$(cat "$CREDENTIALS_DIRECTORY/ai_single_api_key")
fi
if [ -n "$CREDENTIALS_DIRECTORY" ] && [ -f "$CREDENTIALS_DIRECTORY/ai_triage_api_key" ]; then
    AI_TRIAGE_API_KEY=$(cat "$CREDENTIALS_DIRECTORY/ai_triage_api_key")
fi
set +a
exec /usr/bin/java -jar target/foodtime-neo-backend.jar
```

修改单元后执行：

```bash
sudo systemctl daemon-reload
```

## 填写 / 轮换单 key

```bash
cd /root/FoodtimeNeo/FoodtimeNeo-Backend
sudo scripts/security/set-ai-single-key.sh
```

脚本会：

1. 从终端读取 key（不回显、不进 shell 历史）；
2. 用该 key 调一次 `{base-url}/chat/completions` 自检（`--skip-test` 可跳过）；
3. 主机绑定加密写入 `/etc/credstore.encrypted/ai_single_api_key`；
4. 把 `.env` 切到 `AI_SINGLE_ENABLED=true`、`AI_SINGLE_BASE_URL=https://api.deepseek.com`、
   `AI_SINGLE_MODEL=deepseek-flash`（可用 `BASE_URL=` / `MODEL=` 覆盖）；
5. 重启 `foodtime-backend` 并等待健康检查。

只写凭据不重启：`--no-restart`。

## 验证

```bash
systemctl status foodtime-backend --no-pager
# 启动日志应出现：
# AI single-key mode ACTIVE model=deepseek-flash: rotation, escalation, fallback and Redis model map disabled

# 进程环境是否注入（只数个数，不显示值）：
tr '\0' '\n' < /proc/$(systemctl show -p MainPID --value foodtime-backend)/environ \
  | grep -c '^AI_SINGLE_API_KEY='     # 期望 1

# 磁盘上只有密文：
sudo ls -l /etc/credstore.encrypted/
sudo grep -c '^AI_SINGLE_API_KEY=' /root/FoodtimeNeo/FoodtimeNeo-Backend/.env   # 期望 0
```

## 注意

- **主机绑定**：密文只能在本机解密。换服务器、重装系统后需重新填写 key。
- 备份 `/etc/credstore.encrypted/` 到其他机器没有意义；如备份同机，请同时保护
  `/var/lib/systemd/credential.secret`。
- 多 key 回退链路使用 `ai_triage_api_key` 凭据；切回旧链路只需
  `AI_SINGLE_ENABLED=false`，密钥仍由同一机制注入。
- 单 key 模式不允许英文逗号；key 只通过终端输入，不要写进 `.env`、脚本参数或 shell 历史。
