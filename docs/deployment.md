# 生产与容器部署

本地 Compose 提供 PostgreSQL 和 Redis，端口仅绑定本机；生产部署需配置自己的数据库、Redis 与网络访问规则。登录使用Redis服务端会话和HttpOnly Cookie；新增业务接口默认需要登录，管理员接口还须实现对应的服务端角色权限校验。

生产环境必填：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、`REDIS_PASSWORD`。例如在部署平台设置 `SPRING_PROFILES_ACTIVE=prod` 并注入这些变量；远程 PostgreSQL 的 JDBC URL 可配置 `sslmode=verify-full`，Redis TLS 使用 `REDIS_SSL_ENABLED=true`。生产默认仅暴露健康端点，健康详情隐藏，API 文档关闭。

Dockerfile 使用 Java 21 多阶段构建，以非 root 用户运行，容器默认启用 `prod`：

```powershell
docker build -t foodtime-neo-backend:local .
# 创建仅供容器使用、指向可从容器访问的数据库 / Redis 的环境文件
docker run --rm --env-file .env.container -p 8080:8080 foodtime-neo-backend:local
```

容器中的 `localhost` 指向容器本身，因此 `.env.container` 中必须填写实际可达的服务地址，并使用 `SPRING_PROFILES_ACTIVE=prod`。若连接开发机服务，可根据 Docker Desktop 配置使用 `host.docker.internal`。

`.gitignore` 已覆盖 Maven 构建目录、IDE 状态、本地环境配置、日志、运行数据和私钥；保留 `.env.example`、Maven Wrapper 与版本化数据库迁移。`.dockerignore` 将本地凭据、Git 与构建缓存排除在镜像构建上下文之外。
