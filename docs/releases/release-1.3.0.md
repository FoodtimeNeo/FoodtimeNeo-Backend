# v1.3.0 发布说明

后端版本更新为 `1.3.0`，稳定构建文件名仍为 `foodtime-neo-backend.jar`。通过 `dev → main` 发布 PR 保留合并历史。

## 更新范围

- **访客账号与学校邮箱绑定**：注册不再限于学校邮箱；非学校邮箱注册为只读 `visitor`，可在「我的」绑定学号邮箱后升级为 `user`。新增 `POST /api/v1/auth/bind-email/email-code` 与 `POST /api/v1/users/me/bind-email`，个人资料返回 `schoolEmail` / `schoolEmailVerified`。
- **注册邮箱白名单**：只允许主流邮箱（QQ、Foxmail、163、126、新浪、搜狐、运营商邮箱、Gmail、Outlook、iCloud、Yahoo、Proton、学校邮箱等），由 `REGISTRATION_EMAIL_DOMAINS` 配置，允许子域。
- **图片上传上限 50MB**：multipart 单文件 50MB、请求 60MB；超过 10MB 的原图自动降采样并压缩到 5MB 左右，其余图片硬上限 10MB，缩略图仍为 KB 级。
- **菜品 AI 分诊**：菜品与图片上传支持 AI 预检建议，高风险内容自动驳回并保留人工复核记录；管理端支持按类型筛选。
- **平台统计口径**：明确在线用户与日活的统计窗口和去重规则。
- **修复**：个人资料查询列顺序与 `ProfileResponse` 对齐，避免新增字段导致个人资料接口 503。

## 数据库迁移

- `V35__visitor_accounts.sql`：角色约束加入 `visitor`，新增 `school_email` / `school_email_verified_at` 与唯一索引，并为存量 `@bjtu.edu.cn` 用户补绑学校邮箱。
- `V36__catalog_ai_rejection_only.sql`：AI 驳回相关内容（原 V35 重编号，避免与访客迁移冲突）。

Flyway 自动执行，不手工修改版本表。**只有在 2026-11-02 14:57 之后、V35/V36 重编号修复之前部署过 AI 构建的数据库**，才需要按[部署检查清单](../ops/deployment.md#3-迁移版本号变更v35--v36)修正 `flyway_schema_history`。

## 部署注意

- 新增环境变量（可选）：`REGISTRATION_EMAIL_DOMAINS`，默认值已内置主流邮箱列表。
- 上传相关：`UPLOAD_MAX_FILE_SIZE=50MB`、`UPLOAD_MAX_REQUEST_SIZE=60MB`、`REQUEST_MAX_MULTIPART_BYTES=62914560`；Nginx 需 `client_max_body_size 60m`。
- Android 更新：`APP_RELEASE_*` 的 `APP_RELEASE_APK_URL` 指向 GitHub Release 资产时，客户端会读取同目录 `manifest.json` 优先下载增量补丁，缺失或校验失败自动回退完整包。
- 存量非白名单邮箱账号仍可正常登录、找回密码和绑定学校邮箱；白名单只作用于新注册与注册发码。
