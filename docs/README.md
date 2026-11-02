# 后端技术与运维文档导航

本文档是 `FoodtimeNeo-Backend` Spring Boot 4.x / Java 21 后端服务的系统级技术导航与运维操作速查中心。

当前后端工程版本：**v1.3.0**（日常开发主分支为 `dev`，生产发布基线为 `main`）。

---

餐厅与档口独立点评及完整互动：见 [功能与接口说明](modules/venue-reviews.md)。

## 1. 文档结构索引矩阵

本仓库文档遵循四级核心分类，并保留接口规范与架构演进提案目录：

```
docs/
├── ops/             # 运维部署、环境配置、监控告警、凭据管理、压测与种子数据
├── architecture/    # 系统分层架构、数据库设计、Schema 定义与隐私合规
├── modules/         # 业务模块细节（AI 审核、数据看板、社区管理、公告、图片缓存等）
├── releases/        # 生产版本发布说明与历史环境端到端验收记录
├── api/             # 完整 HTTP/REST 接口契约与联调示例（.http 文件）
├── performance/     # 接口压测方案与基准测试报告
└── proposals/       # 历史架构重构与特性演进 RFC 提案
```

| 模块分类 | 核心文档 | 运维与研发说明 |
| :--- | :--- | :--- |
| **运维与部署标准** (`ops/`) | [生产部署手册](ops/deployment.md) | Docker Compose 单机/集群部署、Nginx 反代配置、JVM 堆优化与蓝绿发布 |
| | [环境与运行配置](ops/configuration.md) | 环境变量字典、连接池（HikariCP）、Redis 会话、CORS 与限流配置 |
| | [监控与指标集成](ops/monitoring.md) | Prometheus 采集点、Actuator 探针、Grafana 监控看板口径 |
| | [生产凭据与敏感信息](ops/secrets.md) | systemd 加密凭据注入、API Key 保护与严禁提交敏感信息的防护准则 |
| | [种子数据与图片部署](ops/seed-data.md) | 基础食堂/档口/菜品种子数据导入、图片资源本地落盘与映射规范 |
| | [CI/CD 自动化门禁](ops/ci-cd.md) | GitHub Actions 流水线、自动化构建与集成测试覆盖标准 |
| | [压测指南与基准](ops/load-test.md) | 吞吐量压测脚本、全链路并发瓶颈分析与调优建议 |
| | [Android 本地联调](ops/android-local-integration.md) | ADB 端口映射（reverse）、本地联调证书与会话排查指引 |
| **系统与架构规范** (`architecture/`) | [分层系统架构](architecture/architecture.md) | 模块职责、单向依赖流、领域模型与异常处理契约 |
| | [数据库总体说明](architecture/database.md) | PostgreSQL 核心表、Flyway 迁移规范与评分汇总逻辑 |
| | [数据库设计详细规范](architecture/database-design.md) | 字段类型、约束索引、级联规则与完整物理模型 |
| | [数据库设计符合性审计](architecture/database-design-audit.md) | 接口实现与物理模型一致性核对与审计报告 |
| | [社区模块 Schema](architecture/community-schema.md) | 帖子、楼层回复、二级评论及点赞互动数据模型 |
| | [隐私策略与合规](architecture/privacy-policy.md) | 数据收集范围、存储生命周期与用户数据脱敏规范 |
| **业务功能模块** (`modules/`) | [AI 审核与分诊机制](modules/ai-moderation.md) | 投稿/发帖 AI 审核流水线、分诊建议、拒绝规则与状态流转 |
| | [平台数据看板](modules/admin-dashboard.md) | 在线用户统计、DAU 日活计算口径、趋势聚合与异常兜底 |
| | [社区内容管理](modules/community-management.md) | 违规内容删除、管理员操作日志与个人资料（头像/昵称）管理 |
| | [社区业务接口](modules/community-api.md) | 社区信息流、发帖、回复与评论接口规范 |
| | [社区业务交付记录](modules/community-delivery.md) | 社区模块阶段性交付与多端协同验证记录 |
| | [登录公告机制](modules/notices.md) | 登录弹窗规则、公告版本判定与阅读状态维护 |
| | [公告管理后台](modules/notice-management.md) | 管理端公告发布、Markdown 渲染安全与启停状态 |
| | [媒体资源缓存](modules/media-cache.md) | 服务端静态媒体资源缓存策略与 HTTP 协商缓存 |
| | [密码重置安全](modules/password-recovery-security.md) | 邮箱验证码找回密码安全防护与防爆破频率限制 |
| | [审核提交者详情](modules/moderation-publisher-details.md) | 审核流中展示提交者基础信息与风控辅助数据 |
| | [食堂数据刷新 Batch 1](modules/canteen-refresh.md) | 明湖食堂等第一批基础菜品数据刷新脚本说明 |
| | [食堂数据刷新 Batch 2](modules/canteen-refresh-batch2.md) | 学活、学四等第二批菜品更新与历史数据清理 |
| **版本与交付记录** (`releases/`) | [v1.0.0 发布说明](releases/release-1.0.0.md) | 食堂、档口、菜品基础浏览与首发版本 |
| | [v1.1.1 发布说明](releases/release-1.1.1.md) | 点评互动与安全加固版本 |
| | [v1.1.2 发布说明](releases/release-1.1.2.md) | 社区互动与管理员控制台 |
| | [v1.1.3 发布说明](releases/release-1.1.3.md) | 资料维护与超级管理员权限补齐 |
| | [v1.2.0 发布说明](releases/release-1.2.0.md) | 菜品上传 AI 审核判定与筛选过滤优化 |
| | [v1.3.0 发布说明](releases/release-1.3.0.md) | 访客账号、邮箱白名单、50MB 上传压缩与 AI 分诊 |
| | [测试用例与验证](releases/testing-verification.md) | 单元与集成测试用例清单及执行结果 |
| | [历史工程验收记录](releases/verification.md) | 真实环境端到端验证证据与生产交付报告 |
| **接口与提案** | [API 文档中心](api/README.md) | 完整 RESTful 接口文档与 HTTP 联调脚本 |
| | [架构演进提案](proposals/) | 架构变更讨论与 RFC 提案记录归档 |
| | [性能测试分析](performance/) | 历史并发性能测试方案与报告 |

---

## 2. 运维与生产操作速查手册 (Ops Runbook)

### 2.1 基础运维与生命周期管理

```bash
# 1. 生产环境启动（后台运行）
docker compose -f compose.prod.yml up -d

# 2. 查看容器运行状态与资源占用
docker compose -f compose.prod.yml ps
docker stats

# 3. 查看实时应用日志（追踪最近 200 行）
docker compose -f compose.prod.yml logs -f --tail=200 app
```

### 2.2 健康检查与探针接口

| 探针类别 | 请求端点 | 预期返回 | 作用 |
| :--- | :--- | :--- | :--- |
| **服务就绪（Readiness）** | `GET http://localhost:8080/actuator/health/readiness` | `{"status":"UP"}` | 判定 DB、Redis 依赖就绪，可接入流量 |
| **服务存活（Liveness）** | `GET http://localhost:8080/actuator/health/liveness` | `{"status":"UP"}` | 判定应用线程未卡死，异常时容器自动重启 |
| **详细组件健康状态** | `GET http://localhost:8080/actuator/health` | `{"status":"UP",...}` | 包含 DiskSpace、Postgres、Redis 明细 |
| **Prometheus 指标采集** | `GET http://localhost:8080/actuator/prometheus` | Prometheus 格式文本 | 供监控系统拉取 JVM、HikariCP、HTTP 吞吐指标 |

### 2.3 数据库演进与 Flyway 运维准则

1. **迁移文件命名规范**：必须严格遵循 `V<版本号>__<小写描述>.sql`，版本号只增不减。
2. **禁止修改已执行迁移**：严禁修改任何已在测试或生产环境运行过的迁移文件（会导致校验和不匹配，启动失败）。
3. **修复与回退策略**：若迁移存在缺陷，通过新增高版本迁移（如 `V37__fix_xxx.sql`）进行修正，严禁使用 `flyway:repair` 绕过生产一致性检查。

### 2.4 运行时热更与应急排障

#### ① AI 审核模型热插拔与降级（无需重启应用）
服务在运行时监听 Redis 配置，可在高并发或上游 API 故障时执行平滑降级：
```bash
# 紧急关闭 AI 自动分诊（平滑降级为全人工审核）
redis-cli -a <REDIS_PASSWORD> set ai:triage:enabled false

# 热切换分诊模型（如从 deepseek 切换为 qwen）
redis-cli -a <REDIS_PASSWORD> set ai:triage:model qwen-plus

# 恢复 AI 审核
redis-cli -a <REDIS_PASSWORD> set ai:triage:enabled true
```

#### ② 动态调整日志级别（快速定位故障）
```bash
# 临时将某个包日志级别调至 DEBUG（无需重启）
curl -X POST "http://localhost:8080/actuator/loggers/com.FoodtimeNeo" \
     -H "Content-Type: application/json" \
     -d '{"configuredLevel":"DEBUG"}'

# 恢复默认 INFO 级别
curl -X POST "http://localhost:8080/actuator/loggers/com.FoodtimeNeo" \
     -H "Content-Type: application/json" \
     -d '{"configuredLevel":"INFO"}'
```

#### ③ 磁盘媒体目录与权限核查
- 上传图片默认保存在配置的 `${UPLOADS_DIR}`。
- 确保持久化挂载卷具备应用运行用户的写权限（UID `10001`）。
- 详见 [部署手册](ops/deployment.md) 与 [生产凭据说明](ops/secrets.md)。
