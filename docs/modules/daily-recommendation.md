# FoodtimeNeo Daily HybridRec V2

## 功能与实际架构

后端按北京时间每日生成一份全站统一推荐，默认 10 道菜；没有个性化、行为埋点、人工质量标签或外部 AI 依赖。只改 Backend，沿用 Java 21、Spring Boot 4.1.1、PostgreSQL、Redis、Flyway、JUnit 与现有核心依赖版本。

Controller 使用现有 `ApiResponse`；Service 编排事务与缓存，Store 使用项目已有的 JdbcTemplate 数据访问方式；Generator、Calculator、Diversifier 只计算，不访问数据库。复用已有 SchedulingConfig 与认证 Clock，不新增调度基础设施。

复用 `dishes → stalls → dining_halls`，三层状态都必须为 active。有效评分复用 `dish_stats`（已有触发器维护）及相同的 `dish_rating_summary` 口径，图片只取 approved。正式 `dish_reviews` 中未软删除的记录有效；待审/驳回投稿留在 `dish_review_submissions`，不参与评分或热度。没有另建评分计数器。

## 为什么采用混合推荐与双池

冷启动时多数菜品只有 0～3 条评论，按平均分会放大偶然极端评分，按评论数会使新菜失去机会。混合分同时利用口碑 Q、热度 H、每日轮换 R、历史补偿 E；冷却、配额、多样性由选择阶段独立处理。

口碑池：有效评论数至少 1。探索池：评论数少于 5，或历史窗口内入选次数不超过 1。两池允许重叠，所有阶段共享已选 ID、档口计数和食堂计数。5 条评论只是成熟度统计门槛，不是推荐资格门槛。没有因为单条低分排除探索的规则。

## 贝叶斯口碑与可信度

`Q=(n*avgRating+m*globalAverage)/(n+m)/5`，默认 `m=10`。全站均值由现有 dish_stats 的总 rating_sum / 总 review_count 精确计算，包含当前下架菜品的有效正式评论；没有有效评论才回退到 3.5。单菜平均分用于算法时不先四舍五入。

无评论菜品的 Q 为全站均值 / 5。全站均值 3.5 时，一条五星评论的 Q 约 0.7273，而不是 1。

`confidence=n/(n+m)` 仅说明评论支持程度：0/1/5/10/30 条对应 0/0.0909/0.3333/0.5/0.75，不再乘以 Q，避免再次惩罚新菜。

## 近期热度

`H=ln(1+reviews7d)/ln(1+maxReviews7d)`，最大值为 0 时 H=0。窗口固定为推荐日期之前 7 个完整上海业务日，即 `[D-7 日 00:00,D 日 00:00)`，不包含当天后续新增评论。因此同一天首次生成时间不改变热度窗口。

一条聚合 SQL 按首次 created_at 统计有效正式评论。修改评分或正文不会增加热度，软删除后不再计入。最大热度取当前可见候选的最大值。

## 每日稳定哈希

输入为 UTF-8 编码的 `ISO 日期 + U+000A + 标准 UUID + U+000A + 算法版本`；SHA-256 前 8 字节按大端读 long，无符号右移 11 位保留最高 53 位，再除以 2^53。结果严格在 [0,1)，没有实例地址、Java 对象 hashCode、用户 ID 或请求随机数。

同一输入相同；日期或算法版本变化通常改变结果。同分时按 R 降序，再按 Java UUID 自然顺序升序，输入列表顺序不影响结果。

## 历史曝光补偿

`E=1/(1+n90)`，0/1/2/4 次对应 1/0.5/0.3333/0.2。“曝光”仅代表进入每日快照，不代表实际被用户看到。

n90 使用 `[D-90,D)` 成功批次。最近一次推荐日期则从 D 之前全部成功历史取得，不受该窗口限制；所以更改冷却期不会错误丢失早期最近推荐日期。查询批量聚合，不逐菜查库。失败批次与当天重复执行不累计曝光。

## 动态权重与配额

`coverage=评论数至少5的可见菜品数/全部可见菜品数`；分母 0 时为 0。`alpha=min(coverage/0.5,1)`。Q/H/R/E 的冷启动端点为 0.10/0.05/0.55/0.30，成熟端点为 0.40/0.15/0.25/0.20，按 alpha 线性插值。

| coverage | alpha | Q | H | R | E | 口碑/探索目标 |
|---|---:|---:|---:|---:|---:|---:|
| 0 | 0 | .10 | .05 | .55 | .30 | 0/10 |
| .25 | .5 | .25 | .10 | .40 | .25 | 3/7 |
| .50 及以上 | 1 | .40 | .15 | .25 | .20 | 7/3 |

默认 `qualityQuota=min(7,floor(coverage*15))`。目标数 N 可配置时等比推广为 `min(floor(.7*N),floor(coverage*1.5*N))`。配额不足先转给探索，再允许剩余口碑候选补位；候选本身不足时配额不会强行补出菜品。推荐类型由实际入选的池阶段决定，后续混合回填按探索资格分配 EXPLORE，否则为 QUALITY。

两个权重端点均集中在配置中；禁止负数、NaN、Infinity 或总和不为 1。参数、端点、算法版本、窗口和调度设置生成 SHA-256 配置摘要，保存到批次。已有成功批次即使配置改变也继续复用，避免同日多版本。

## 冷却、确定性贪心与放宽次序

最近 30 天为 `[D-30,D)`，边界 D-30 仍在冷却中，D-31 已可用。

1. 非冷却候选按口碑与探索配额选择，遵守档口/食堂上限。
2. 非冷却候选放宽池配额，仍遵守上限。
3. 每轮把两个上限各增加 1，直到达到数量或上限达到目标数。
4. 所有可用非冷却候选仍不足时，才从冷却候选回填；优先上次推荐日期最早的候选，再按覆盖与分数选择，并按相同方式放宽上限。
5. 总菜品不足则返回实际数量，绝不重复 dishId。

每次选择都重新计算覆盖优先级：食堂覆盖不足 minimumHalls 时优先未覆盖食堂，再适度优先未覆盖档口，随后按综合分降序、R 降序、UUID 升序。默认至少争取 3 个食堂、每档口最多 2 道、每食堂最多 4 道；候选不足时按上述次序放宽。这些是业务约束，不是永久数据库限制。

## 数据库、事务与并发

`V38__daily_recommendations.sql` 新增两表，不改 V1～V37：

- daily_recommendation_batch：日期唯一，保存算法版本、配置摘要、generating/success/failed 与时间。
- daily_recommendation_item：菜品外键 RESTRICT、批次外键 CASCADE、批次内菜品和位置唯一，位置从 1 开始；保存类型、分数、理由。

日期唯一索引支持批次读取；(batch_id,position) 唯一索引支持有序明细读取；(dish_id,batch_id) 索引支持历史连接，没有重复创建相同索引。

显式 TransactionTemplate 使用 REQUIRES_NEW。无论定时任务还是 API 补算，都先 INSERT ON CONFLICT DO NOTHING，再 SELECT FOR UPDATE 持有日期行锁；检查成功结果后才生成、写明细、标记 success。并发实例会等待同一行并复用成功批次。成功快照不会由生成服务覆盖。

异常回滚整个明细事务，再用单独事务记录 failed。失败标记使用条件更新，绝不覆盖其他实例已成功的结果；数据库本身断开时可能连失败标记也无法保存，原错误继续交给现有异常处理，下次请求可重试。只有 success 批次可被接口或历史查询读取。

## Redis 缓存

Key：`foodtime:recommendations:daily:v2:{yyyyMMdd}`。值：`batchUUID|逗号分隔的dishUUID列表`，不缓存用户和完整实体。默认 TTL 172800 秒，配置不得少于 2 天。

缓存仅在数据库事务返回、确认提交之后写入。缓存异常或损坏值当作未命中。即使命中，仍查 PostgreSQL 成功批次并校验批次 ID；明细始终连接当前菜品/档口/食堂与审核图片。缓存保留轻量身份和 ID，当前设计优先保证一致性，并不消除数据库读取。没有 KEYS 扫描或 Redis 锁依赖。

## 定时任务与异常降级

复用已有 SchedulingConfig，每日 `0 5 0 * * *`、`Asia/Shanghai` 触发。注入 Clock 后统一转换上海日期，测试覆盖 UTC 日期边界。

定时任务只触发服务并记录错误；接口先查成功快照，缺失时执行同样受锁保护的同步生成。失败后可再次请求或再次调度，不返回半成品。日志含日期、菜品数、版本、耗时，不含用户信息。

Redis 故障不阻断推荐；PostgreSQL 故障沿用现有 DATA_UNAVAILABLE/503。空候选生成合法成功空快照。推荐后菜品下架、档口/食堂停用会从响应过滤，允许少于目标数，不重写当天历史。图片撤销审核立即不再显示。

## REST API 与推荐理由

`GET /api/v1/recommendations/daily`，沿用现有登录会话，访客账号也遵循现有认证规则。没有改变结果的 userId 参数。完整包装、字段和示例见 [接口文档](../api/recommendations.md)。

GOOD_REPUTATION：至少 5 条评论且平滑质量 Q≥.75；RECENTLY_POPULAR：该批次热度窗口内评论>0 且 H≥.5；LESS_EXPOSED：生成前历史曝光为 0；否则 DAILY_DISCOVERY。这些说明基于批次生成时的证据，不作“必吃”等保证。当前评分支持不足时，接口会把已不受支持的 GOOD_REPUTATION 降为 DAILY_DISCOVERY。recommendationScore 是生成时综合分，不是原始评分；averageRating/reviewCount 和图片在读取时刷新。

## 配置与本地运行

前缀 `app.recommendation.daily`：

| 参数 | 默认 | 校验/说明 |
|---|---|---|
| algorithm-version | FT-HybridRec-v2 | 非空，最多64字符 |
| target-size | 10 | 正整数 |
| cooldown-days / history-days / recent-days | 30 / 90 / 7 | 冷却可为0，其余正整数 |
| smoothing-prior | 10 | 正整数 |
| max-per-stall / max-per-hall | 2 / 4 | 正整数 |
| minimum-halls | 3 | 非负，选择时最多取目标数 |
| cache-ttl | P2D | 至少2天 |
| cron | 0 5 0 * * * | 合法 Spring cron |
| zone | Asia/Shanghai | 固定业务时区，其他值启动失败 |
| cold-weights / mature-weights | 上述两组端点 | 各含 quality/heat/rotation/exposure，有限非负，总和1 |

常规参数支持 application.yml 中的 RECOMMENDATION_* 环境变量；端点可通过应用配置或 Spring 标准环境变量覆盖（如 APP_RECOMMENDATION_DAILY_COLDWEIGHTS_QUALITY）。所有实例应使用同一配置。

按 [运行配置](../ops/configuration.md) 准备本地 PostgreSQL、Redis 和 .env，再运行 `./mvnw.cmd spring-boot:run`。Flyway 自动迁移，不手改版本表。完整构建为 `./mvnw.cmd -B -ntp -Pintegration verify`。集成测试应使用独立测试数据库（迁移会装入正式种子菜谱；推荐测试临时停用并恢复它们）和独立 Redis DB。

## 单元、数据库和接口验证

`RecommendationAlgorithmTest` 验证公式、置信程度、热度、曝光、哈希、成熟度、非法权重、配额、双池去重、30天边界、先放宽多样性再回填冷却、最早日期回填、空/小菜谱与输入乱序稳定性。`DailyRecommendationCacheTest` 验证损坏缓存、TTL、日期切换和调度失败后再次触发。

`DailyRecommendationIT` 使用真实 PostgreSQL/Redis/HTTP：同日幂等、6个独立实例并发、部分写入回滚与重试、提交后才缓存、Redis故障、当前可见性/图片、待审评论排除、评分更新/软删、独立历史窗口、数据库约束、不同用户相同业务数据、未登录401、空快照和上海日期边界。`DatabaseSchemaIT` 增加两张表的断言。

执行 `./mvnw.cmd "-Dtest=RecommendationAlgorithmTest,RecommendationSimulationTest,DailyRecommendationCacheTest" test` 可独立复跑算法、缓存和模拟；集成验证使用 `./mvnw.cmd -Pintegration "-Dit.test=DailyRecommendationIT,DatabaseSchemaIT" verify`。完整执行记录见 [验收报告](../releases/daily-recommendation-verification.md)。

## 90 天模拟方法与实际结果

`RecommendationSimulationTest` 直接调用生产 Generator。1200 道固定 UUID 菜品，6 食堂、36 档口；75%无评论，20%有1～3条，4%有5～10条，1%有30～49条。评分固定4，近期数量按确定性分布生成。每天先从之前入选日期计算 n90 和最近推荐日期，再运行完整冷却、配额与多样性算法，最后更新历史；并非只重复运行静态打分。

2026-11-03 实测，模拟日期 2026-01-01～03-31，默认配置：

| 指标 | 结果 |
|---|---:|
| 推荐总量 / 每日不同菜品 | 900 / 10 |
| 最近30天重复次数与比率 | 0 / 0% |
| 不同菜品 / 90天覆盖率 | 900 / 75% |
| 每日食堂覆盖 最少 / 平均 | 4 / 5.3667 |
| 每日档口覆盖 最少 / 平均 | 10 / 10 |
| 无评论菜品入选占比 | 66.7778% |
| QUALITY / EXPLORE 入选占比 | 0% / 100% |
| 每菜次数分布 | 300道0次，900道1次 |

此分布只有5%菜品达到5条评论，qualityQuota=floor(.05*15)=0；所以全部以 EXPLORE 阶段入选，包含有少量评论或低曝光的菜品，并不意味着忽略口碑分。成熟配额另有独立测试。90天共900个位置，1200道菜的理论最高覆盖率恰为75%；这次达到该上限。结果只证明本模拟输入的行为，不代表线上流量或任意评论分布保证。

机器可读结果为 `target/recommendation-simulation.json`，含全部90天的菜品ID、食堂/档口覆盖与统计，可通过测试重新生成；[保存的摘要](../releases/daily-recommendation-simulation.json) 便于代码审查。

## 性能与后续扩展

千级候选一次聚合读取，历史一次聚合，内存评分 O(N)，贪心选择默认约 O(N×10)，放宽阶段仍是小常数轮数；明细最多10次插入，均在同一事务，没有逐菜查询评论。图片 LATERAL 复用 dish_id 索引，接口读取最多10个菜品当前信息。

近期评论聚合可能在评论量很大时扫描更多数据；长期历史的 MAX 查询也会逐年增长。应根据 EXPLAIN 和真实负载再增加 created_at 部分索引、历史汇总或缩短事务内读取时间。当前没有线上压测；并发数据库测试证明幂等，不等同吞吐量测试。

后续可评估负面口碑保守规则、季节性先验、离线覆盖报表与更大菜谱性能；任何调整继续记录配置摘要与算法版本，不加入个性化或额外基础设施。
