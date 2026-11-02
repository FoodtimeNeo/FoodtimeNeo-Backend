# 1.0.0 发布验收

项目 Maven 版本统一为 1.0.0。社区帖子、多图、紧凑楼中楼、点赞收藏、完整人工审核与权限隔离随 dev 向 main 的发布 PR 一并交付，数据库沿用不可变的 V1–V23 迁移。

本次修复发布验证中的两项问题：

- 数据库表清单测试更新为 V21 后实际使用的 `community_post_likes`、`community_comment_likes`。
- RegistrationIT 为 SMTP EHLO 和 MIME Message-ID 指定本地测试身份，避免本机反向 DNS 查询使本地 SMTP 夹具超时。继续使用真实 Jakarta Mail 传输和 loopback SMTP，不跳过邮件协议及失败回滚验证。

CI 恢复执行包含 RegistrationIT 的完整集成测试集。

## 本机验证

2026-10-24，Java21、独立 PostgreSQL17.6 / Redis 下运行 Maven `-Pintegration verify`，215项单元测试、217项集成测试全部通过，0失败、0错误、0跳过。邮件独立16项也通过；全量计数已包含这16项，不重复累加。

全量检查首轮误将测试环境的媒体清理开关关闭，导致专门验证清理的 StagedImageCleanupIT 无法装配。启用隔离测试目录的清理后重新执行全量检查通过；没有关闭断言或跳过该测试。开发运行环境的清理仍按原配置保持关闭。

检查使用独立回归数据库和随机临时媒体目录，没有修改原手工测试库。原始检查日志位于联合工作区 `qa/release-1.0.0/`。此结果不代表外部邮件供应商投递或生产数据库升级已验收；正式邮箱投递继续按既有约定另行处理。main 合并不触发服务器部署。
