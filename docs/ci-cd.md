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

未指定服务器时，CD 采用镜像持续交付：`.github/workflows/cd.yml` 在 `Backend CI` 完成后接收事件，只有本仓库 main 的 push、CI成功且存在合并依据时才发布。支持现有本地 `git merge --no-ff` 后推送 main 的流程，也支持 GitHub PR 的 squash/rebase 合并；没有合并依据的直接提交不发布。旧提交已被新的 main 头替代时跳过。

发布构建检出的精确SHA与通过CI的提交一致，使用已有Dockerfile及非root Java 21运行镜像，发布到 `ghcr.io/foodtimeneo/foodtimeneo-backend:sha-<完整提交SHA>`。只发布对应提交标签，不覆盖浮动latest标签。发布任务使用内置 `GITHUB_TOKEN` 的 `packages: write`，无需在仓库保存生产凭据；组织需要允许GitHub Actions写入Packages。

dev 与 PR 检查不会发布镜像。该配置提供可部署镜像，不自动更新未指定的服务器。运行容器仍需按 [部署说明](deployment.md) 配置数据库、Redis和生产环境变量。CI上传JAR与CD发布镜像都不代表服务器已经部署。

工作流落入 dev 后即可运行 CI；`workflow_run` 要求工作流位于默认分支，因此 main 的代码与两个工作流仍需按项目发布流程合并后才会启用CD。不能为了验证CD自动将dev合并到main。本次在dev校验CD语法及发布条件，不声称已经执行镜像推送或服务器部署。

## 参考

- [GitHub 工作流触发与语法](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax)
- [PostgreSQL 测试服务](https://docs.github.com/en/actions/tutorials/use-containerized-services/create-postgresql-service-containers)
- [容器镜像发布](https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images)
