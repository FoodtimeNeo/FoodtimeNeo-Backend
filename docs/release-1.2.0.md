# v1.2.0 发布说明

后端版本更新为 `1.2.0`，稳定构建文件名仍为 `foodtime-neo-backend.jar`。通过 `dev → main` 发布 PR 保留合并历史。

## 更新范围

- **用户关注**：新增 `user_follows` 表与关注、取消关注、关注状态、粉丝列表、关注列表接口；个人资料返回关注与粉丝计数。
- **评论实时展示**：提交评论返回完整对象（含审核状态），新增评论状态查询接口，供客户端轮询审核结果。
- **应用内更新信息**：新增公开接口 `GET /api/v1/system/app-release`，通过 `APP_RELEASE_*` 环境变量配置最新版本与下载地址；未配置时不提示更新。
- 社区评论提交与删除的交互完善（管理删除无需填写原因，保留软删除审计）。
- 邮箱找回密码与一次性重置凭证。
- 生产 CORS 与证书续期配置收口，清理旧域名并补充 www 入口文档。

## 数据库迁移

本次新增 `V34__user_follows.sql`。部署沿用现有数据库与 Redis，Flyway 自动处理迁移；不手工修改数据库版本表。

## 部署注意

- 新增环境变量（按需）：`APP_RELEASE_VERSION_CODE`、`APP_RELEASE_VERSION_NAME`、`APP_RELEASE_APK_URL`、`APP_RELEASE_NOTES`、`APP_RELEASE_MIN_VERSION_CODE`
- 发布新版本时先把 APK 放到 Web 的 `/downloads/`，再更新上述变量并重启
