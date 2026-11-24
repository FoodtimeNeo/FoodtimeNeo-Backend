# GitHub 自动部署

`main` 上的 `pom.xml` 版本号发生变化时，`Backend Deploy` 工作流会自动测试、打包并部署到生产服务器。普通文档修改或未改版本号的合并不会触发部署。

## 触发方式

- 自动：合并带版本号变更的发布 PR 到 `main`。
- 手动：Actions → Backend Deploy → Run workflow。

## 仓库 Secrets

| Secret | 说明 |
| --- | --- |
| `DEPLOY_HOST` | 生产服务器地址 |
| `DEPLOY_PORT` | SSH 端口 |
| `DEPLOY_USER` | 受限部署用户 |
| `DEPLOY_SSH_KEY` | CI 专用私钥 |
| `DEPLOY_KNOWN_HOSTS` | 服务器主机公钥 |

## 服务器侧行为

CI 通过 SSH 强制命令调用：

```text
/usr/local/bin/foodtime-deploy backend <run_id> <commit>
```

该脚本只接受 `status`、`backend`、`web` 三个动作，并会：

1. 备份数据库、当前 jar 和 `.env` 到 `/var/backups/foodtime/`。
2. 校验 workflow run 的 `head_sha` 与传入 commit 一致。
3. 下载 `backend-jar` artifact。
4. 合并 `release.env` 中公开的 `APP_RELEASE_*` 参数。
5. 重启 `foodtime-backend`，等待健康检查；失败时自动回滚 jar 和 `.env`。

部署日志：`/var/log/foodtime-deploy.log`。

数据库迁移由 Flyway 在启动时执行；迁移不可通过降级 jar 自动回滚，失败时需使用备份恢复。
