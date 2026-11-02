# 并发稳定性压测（2026-10-18，本地）

目标：上线前摸清各接口在饭点并发下的表现，确认无 500、无连接池耗尽、无慢请求超标。
脚本与原始结果在仓库外的 `test/load/`（`load.py` + `results-*.json`），复跑方式见该目录 `README.md`。

## 交互特征（决定压测配比）

约 40 个接口中，高频的只有读链路：`GET /dining-halls` → `stalls` → `dishes` →
`dish` + `reviews` + `rating` + `images`，以及 `GET /media/{id}?size=thumb`。
Web/Android 均无轮询，请求全部由用户操作触发。写接口（点评/投稿/上传/改密/登录）都有
用户级 + IP级双限流。分页 `size` 上限 100，超限直接 400，无放大攻击面。

## 历史场景与结果（本机同时跑后端+PG+Redis+压测客户端）

| 场景 | 并发 | 结果 |
| --- | --- | --- |
| 浏览风暴（读链路全套） | 50 VU × 90s | 657 rps，100% 200，p50 ~70ms，p95 ~130ms |
| 浏览风暴 | 150 VU × 60–90s | ~600–636 rps，100% 200，p50 ~230ms，p95 ~400ms，无 500/超时 |
| 公开缩略图 | 30 VU × 60s | 610 rps，100% 200，p95 ~76ms（不可变缓存 + ETag 生效） |
| 登录风暴 | 50 并发 × 3 轮 | 100×200 + 50×429（IP 桶按设计跳闸），Argon2id 在争抢下 p50 ~0.6s、p95 ~0.9s |
| 评论写入突发 | 30 用户 × 15 条 | 189×202 + 259×429 + 2×409，无 500；限流只回 429 + Retry-After |

DB 连接池在 150 并发下仅用 1–4/10 连接、pending 恒为 0、超时计数为 0；
慢请求日志（阈值 2s）全程 0 条。50→150 并发的延迟上涨来自本机 CPU 争抢
（Java 进程 140–215%，同机还有 PG/Redis/压测客户端），不是服务端瓶颈。

以上为 10 月 18 日历史记录，不能直接推断生产容量。当前点评互动专项复验见下文；生产部署的硬件、数据和代理均可能改变结果。

## 压测中发现并已修复的两项上线风险

1. **反向代理后全校共用一份 IP 配额（必修）。** 限流 IP 取自连接地址，
   `FORWARD_HEADERS_STRATEGY` 默认 `none`；代理部署若不改，后端看到的全是代理 IP，
   整校分登录每 15 分钟 1000 次。修复：`deployment.md` 新增反向代理一节——
   代理覆写 `X-Forwarded-For` 并丢弃客户端自带值，后端设 `framework`；
   直连保持 `none`。
2. **每 IP 默认配额按单机设计，扛不住统一出口。** 登录 100→1000、投稿 200→2000、
   上传 100→1000、改密 50→500（窗口不变）；单人滥用仍由用户级配额卡住
   （登录每账号 10 次、投稿每用户 20 次、上传每用户 10 次）。

## 上线前检查单

- [ ] 按 `deployment.md` 反向代理一节配置 Nginx 与 `FORWARD_HEADERS_STRATEGY=framework`
- [ ] 按出口 IP 集中程度复核 `*_IP_LIMIT`，只调大 IP 级别，不动用户级
- [ ] 饭点首周盯 `monitoring.md` 的 p95 与 429 比例，429 激增先查 IP 桶而非扩容
- [ ] 登录是 CPU 最贵的接口（Argon2id）；突发登录排队属正常，p95 < 2s 即达标

## 2026-10-21 点评互动专项复验

在独立本地 `foodtime_completion_lab` 数据库和 18080 服务上，用真实 HTTP、Cookie、CSRF 及 PostgreSQL 18/Redis 运行；原本地库和 8080 服务未参与写入。连接池上限 10。数据为同一热门菜品的 5001 条正式评论、同一主评论的 100 条公开回复及 150 个活跃压测用户。脚本见 [review-load-test.py](../scripts/review-load-test.py)，[原始聚合结果](performance/review-load-2026-10-21.json)。

| 场景 | 并发 / 时长 | 请求数 | rps | p95 | 错误 |
| --- | --- | --- | --- | --- | --- |
| 评分、首屏评论、首屏回复循环读取 | 150 / 20s | 31240 | 1558.16 | 159.65ms | 0 |
| 同一主评论及回复的点赞/取消 | 150 / 20s | 42075 | 2097.69 | 114.07ms | 0 |
| 读写混合（约 60% / 40%） | 150 / 20s | 36732 | 1832.00 | 132.75ms | 0 |

所有请求均 200。脚本校验公开总数、每页上限 20、点赞状态、非负计数与版本不倒退。清理压测点赞后，数据库明细与汇总一致：评论数 5001、评分和 20004、公开回复数 100，两类点赞均 0。Hikari 超时计数为 0，结束时 active/pending 均为 0；只有前后快照，不能据此声称 pending 全程为 0。临时服务日志未发现死锁及超过 2s 的慢请求警告。

本轮仅在测试进程将 `REVIEW_ACTION_USER_LIMIT`、`REVIEW_ACTION_IP_LIMIT`、`AUTH_LOGIN_IP_LIMIT` 设置为 1000000，避免限流掩盖数据库测试；生产默认值未改变。每个场景仅 20 秒，不是长时间稳定性测试，也不是生产 TPS 承诺。热行竞争仍应按生产数据和持续负载监测。

### 复现

1. 新建独立、可丢弃且名称以 `completion_lab` 结尾的 PostgreSQL 库，不要指向现有开发或生产库。在该库启动 V18 后端、独立 Redis 命名空间和 `local-integration` 样例账号；停用外部 AI 和定时清理。
2. 生成待检查 SQL：`python scripts/review-load-fixtures.py --database foodtime_completion_lab --output-directory tmp/review-load`。脚本只生成文件，不连接数据库；SQL 会校验数据库名和本地样例，插入新数据，不替换已有数据。
3. 审查 `tmp/review-load/seed.sql` 后，以 `psql -v ON_ERROR_STOP=1 -d foodtime_completion_lab -f tmp/review-load/seed.sql` 执行。账号密码哈希复制自该库的本地样例账号，不读取外部数据库。
4. 在环境变量 `FOODTIME_LOAD_PASSWORD` 设置该本地样例密码。执行 `python scripts/review-load-test.py --base-url http://127.0.0.1:18080/api/v1/ --fixtures tmp/review-load/fixtures.json --output tmp/review-load/result.json --concurrency 150 --seconds 20 --confirm-isolated-writes`。脚本只接受 loopback 目标及明确写入参数；不要通过端口转发连接生产服务。
5. 结束后比对 `dish_stats` 与有效评论的数量/评分和、`dish_review_stats` 与点赞/公开回复明细、`dish_review_reply_stats` 与回复点赞明细。保留报告后可由操作者删除整个专用测试库。

### 旧库升级验收

`ReviewUpgradeIT` 分别创建随机命名的专用数据库，从 V6（旧 main）和 V14 升级到 V18，带入两条历史评论并检查正文、创建时间、均分、空评分、计数回填、升级后点赞/回复/软删除。只清理本测试随机创建的库，不清理配置中的测试库；执行角色需要 CREATEDB 权限。

本轮 `-Pintegration '-Dit.test=*IT,!RegistrationIT' verify`：215 项普通测试及 191 项集成测试全部通过。按既定范围排除邮件注册集成测试，未用本地验收代替生产迁移、真实邮件投递或真实 AI 提供方验收。
