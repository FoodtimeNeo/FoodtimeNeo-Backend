# CI/CD

## CI

`.github/workflows/ci.yml` 使用 GitHub Actions，面向 `main`、`dev` 的 Pull Request 以及这两个分支的 push 都执行检查。现有直接合并并推送 dev 的流程会触发 CI；feature 分支单独 push 不触发。

运行环境为 Ubuntu 24.04、Temurin Java 21、项目 Maven Wrapper、PostgreSQL 17 和 Redis 7.4。数据库和 Redis 是每次运行独立的测试服务，不使用部署凭据或现有业务数据库。CI 执行普通测试、Flyway、数据库约束及 HTTP 集成测试，成功后上传可运行 JAR；测试报告无论成功失败都会上传。

```sh
bash ./mvnw -B -ntp -Pintegration '-Dit.test=*IT,!RegistrationIT' verify
```

已有 `RegistrationIT` 的邮箱集成尚未接通，因此此组测试明确排除。普通测试仍覆盖注册和验证码逻辑；CI 成功不代表 SMTP 投递通过。邮件流程修复并验证后，应移除排除项。

第三方 Actions 固定到官方发布对应的完整提交 SHA。CI 默认只拥有仓库读取权限，不使用生产 Secrets。语法可通过 `actionlint .github/workflows/*.yml` 检查。

## CD

CD 只允许在 main 合并后执行，dev 和面向分支的 PR 检查不执行发布或部署。具体发布目标、部署方式及所需凭据待确认后配置；CI 的 JAR 上传仅作为构建产物，不表示已部署。

工作流落入 dev 后即可运行 CI；main 的代码与工作流仍需按项目发布流程合并，不能为了验证 CD 自动将 dev 合并到 main。

## 参考

- [GitHub 工作流触发与语法](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax)
- [PostgreSQL 测试服务](https://docs.github.com/en/actions/tutorials/use-containerized-services/create-postgresql-service-containers)
- [容器镜像发布](https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images)
