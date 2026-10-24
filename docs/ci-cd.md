# CI/CD

## CI

`.github/workflows/ci.yml` 使用 GitHub Actions，面向 `main`、`dev` 的 Pull Request 以及这两个分支的 push 都执行检查。现有直接合并并推送 dev 的流程会触发 CI；feature 分支单独 push 不触发。

运行环境为 Ubuntu 24.04、Temurin Java 21、项目 Maven Wrapper、PostgreSQL 17 和 Redis 7.4。数据库和 Redis 是每次运行独立的测试服务，不使用部署凭据或现有业务数据库。CI 执行普通测试、Flyway、数据库约束及 HTTP 集成测试，成功后上传可运行 JAR；测试报告无论成功失败都会上传。

```sh
bash ./mvnw -B -ntp -Pintegration verify
```

1.0.0 起，`RegistrationIT` 使用不依赖本机 DNS 的 loopback SMTP 夹具，已恢复到完整 CI。该测试验证真实邮件传输、注册及失败回滚；CI 成功不代表外部邮件供应商实际投递通过。完整本机回归见 [1.0.0 发布验收](release-1.0.0.md)。

第三方 Actions 固定到官方发布对应的完整提交 SHA。CI 默认只拥有仓库读取权限，不使用生产 Secrets。语法可通过 `actionlint .github/workflows/*.yml` 检查。

## CD

按用户最新选择，CD 当前仅预留，暂不启用。模板保存在 `.github/workflow-templates/cd.yml`，不位于 GitHub Actions 自动加载的 `.github/workflows` 目录，因此即使合并到 main 也不会自动发布镜像或部署服务。

预留模板供后续确定交付目标时参考：它只允许本仓库 main 合并提交通过CI后进入发布，支持本地 `git merge --no-ff` 和 GitHub PR 的 squash/rebase 合并；直接提交、失败CI和旧main提交跳过。模板检出的提交与通过CI的SHA一致，候选交付方式是用已有Dockerfile构建GHCR镜像，并保留对应SHA标签。该候选方式当前不会执行，也不代表已确定最终部署目标。

以后启用时，需先明确镜像仓库或服务器、相应权限和生产配置；再在功能分支修改模板并移入 `.github/workflows/cd.yml`，按发布流程合并到main。模板的 `workflow_run` 触发方式要求文件位于默认分支。启用前仍需保持“仅main合并且CI成功”的条件。当前不配置生产Secrets，不执行镜像推送或服务器部署。

CI继续在main/dev的PR和push时执行，上传JAR仅为测试后的构建产物。预留模板可用 `actionlint .github/workflow-templates/cd.yml` 校验；语法校验不启用CD。运行容器的配置见 [部署说明](deployment.md)。

## 参考

- [GitHub 工作流触发与语法](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax)
- [PostgreSQL 测试服务](https://docs.github.com/en/actions/tutorials/use-containerized-services/create-postgresql-service-containers)
- [容器镜像发布](https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images)
