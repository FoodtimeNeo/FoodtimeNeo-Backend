# Backend 文档导航

本文档按当前源码维护；带日期的验收记录只证明当时的环境与范围。修改功能时同步更新对应接口、配置和验证说明，不把未合入的实验写成已交付能力。

| 需要了解的内容 | 入口 |
| --- | --- |
| 安装、启动与常用检查 | [项目 README](../README.md) |
| 分层、目录和事务边界 | [架构](architecture.md) |
| 环境变量、Cookie、存储与连接 | [配置](configuration.md) |
| 当前数据库字段与约束 | [数据库设计](database-design.md) |
| 迁移版本与评分快照 | [数据库与迁移](database.md) |
| 菜单、食堂封面及图片文件落盘 | [种子数据与图片部署](seed-data.md) |
| 方法、路径、DTO及错误约定 | [API 目录](api/README.md) |
| 数据库设计与实现的对应关系 | [设计核查](database-design-audit.md) |
| 开发环境注册测试码 | [测试验证码](testing-verification.md) |
| 生产容器、持久化与反向代理边界 | [部署](deployment.md) |
| 指标采集、慢请求与磁盘盯防 | [监控](monitoring.md) |
| GitHub 检查和预留发布模板 | [CI/CD](ci-cd.md) |
| 隔离环境历史验证 | [验收记录](verification.md) |
| Android 专用本地环境 | [Android 本地联调](android-local-integration.md) |

接口以 Controller、DTO、Service 和数据库迁移为核对来源；配置以 `application*.yml`、配置类、Compose 和 Dockerfile 为核对来源。数据库保存了图片引用不代表运行环境已经放置图片字节；CI通过不代表正式邮件或线上部署已验收。

本轮文档基线为 `main` 的 `f3c8638`（包含 V6 食堂封面迁移）；先将 `dev` 快进到该基线，再在文档功能分支维护。已有迁移保持不变，后续迁移从 V9 递增。
