# Daily HybridRec V2 交付与验收记录

验收日期：2026-11-03。范围仅 FoodtimeNeo-Backend。已从 origin/dev 执行 fetch 与 ff-only pull，基线为 `9dfa18a1082238723b88654f261415c459c16b96`，功能分支为 `feature/daily-hybridrec-v2`。没有推送、合并或生产部署，未修改 Web、Android、历史迁移或核心依赖版本。

## 1. 新增文件

以下路径均相对 Backend 仓库根目录：

```text
src/main/java/com/FoodtimeNeo/api/recommendation/DailyRecommendationController.java
src/main/java/com/FoodtimeNeo/config/properties/DailyRecommendationProperties.java
src/main/java/com/FoodtimeNeo/recommendation/DailyRecommendationCacheService.java
src/main/java/com/FoodtimeNeo/recommendation/DailyRecommendationGenerator.java
src/main/java/com/FoodtimeNeo/recommendation/DailyRecommendationScheduler.java
src/main/java/com/FoodtimeNeo/recommendation/DailyRecommendationService.java
src/main/java/com/FoodtimeNeo/recommendation/DailyRecommendationStore.java
src/main/java/com/FoodtimeNeo/recommendation/RecommendationDiversifier.java
src/main/java/com/FoodtimeNeo/recommendation/RecommendationModels.java
src/main/java/com/FoodtimeNeo/recommendation/RecommendationScoreCalculator.java
src/main/java/com/FoodtimeNeo/recommendation/RecommendationWeightCalculator.java
src/main/java/com/FoodtimeNeo/recommendation/dto/DailyRecommendationResponse.java
src/main/java/com/FoodtimeNeo/recommendation/dto/RecommendedDishResponse.java
src/main/resources/db/migration/V38__daily_recommendations.sql
src/test/java/com/FoodtimeNeo/integration/DailyRecommendationIT.java
src/test/java/com/FoodtimeNeo/recommendation/DailyRecommendationCacheTest.java
src/test/java/com/FoodtimeNeo/recommendation/RecommendationAlgorithmTest.java
src/test/java/com/FoodtimeNeo/recommendation/RecommendationSimulationTest.java
docs/api/recommendations.md
docs/modules/daily-recommendation.md
docs/releases/daily-recommendation-simulation.json
docs/releases/daily-recommendation-verification.md
```

## 2. 修改文件

- `src/main/resources/application.yml`：推荐参数及两组权重端点。
- `src/test/java/com/FoodtimeNeo/integration/DatabaseSchemaIT.java`：表集合增加两张推荐表。
- `docs/README.md`：模块导航入口。
- `docs/api/README.md`：API 文档入口。

## 3. 核心算法与动态权重

Q 贝叶斯平滑现有有效评分，H 聚合前7个完整上海业务日的新增有效评论，R 使用明确编码/字节序的SHA-256确定性轮换，E 为过去90天入选次数补偿。成熟度 coverage 按至少5条评论的可见菜品占比计算，alpha=min(coverage/.5,1)，四权重线性插值。

coverage=0 时 Q/H/R/E=.10/.05/.55/.30；coverage=.25 时为 .25/.10/.40/.25；coverage≥.5 时为 .40/.15/.25/.20。默认口碑配额分别为0、3、7，其他位置用于探索。有效评论至少1条即可进入口碑池，不把5条设为资格门槛。

两池共享去重、冷却和多样性计数。先非冷却候选按配额选择，再放宽配额，再逐级增加档口/食堂上限，最后才按最近推荐日期最早回填冷却候选。默认10道、档口2道、食堂4道、争取至少3食堂。同分按R与UUID稳定排序。公式、边界、配置校验与理由规则详见 [模块文档](../modules/daily-recommendation.md)。

## 4. 数据库迁移与调度

V38 新增日期唯一的批次表，以及批次内 dish/position 唯一的明细表；提供外键、状态、分数、理由与位置约束及历史索引。沿用已有 dish_stats 评分触发器，没有新计数体系。

生成使用 TransactionTemplate、PostgreSQL INSERT ON CONFLICT 和日期行锁。明细与 success 在同一事务提交，失败回滚后独立记录failed；失败标记不会覆盖并发成功结果。成功批次总是复用。6个独立服务对象并发集成测试确认一个日期只有一个完整成功批次。

复用 SchedulingConfig；每天上海00:05触发。Clock统一上海日期。调度失败记录日志，API缺少成功快照时同步补算，复用相同事务路径。

## 5. Redis 与 API

Key=`foodtime:recommendations:daily:v2:{yyyyMMdd}`，默认TTL=172800秒，保存批次UUID和菜品UUID列表。提交后才写缓存；损坏或故障降级查PostgreSQL，命中仍校验成功批次和当前公开可见性。当前实现优先一致性，缓存并不取消数据库读取。

`GET /api/v1/recommendations/daily` 使用现有会话和ApiResponse，无个性化参数。每项包含菜品/食堂/档口、当前评分、审核图片、QUALITY/EXPLORE、生成时综合分及理由。菜品/父级停用后过滤，允许不足10道，原历史不改写。

[完整REST示例、字段与错误](../api/recommendations.md)。未登录401，空候选200空列表，数据库故障沿用503/DATA_UNAVAILABLE。

## 6. 测试执行结果

最终全量回归通过，全部零失败、零错误、零跳过：

| 检查 | 实测结果 |
|---|---|
| Maven 单元测试 | 358通过 |
| Maven PostgreSQL/Redis/HTTP 集成测试 | 304通过 |
| 其中新推荐单元/模拟测试 | 27通过（算法24、缓存/调度2、模拟1） |
| 其中新推荐集成测试 | 13通过 |
| 定向推荐+数据库结构集成测试 | 29通过 |
| Spring Boot 可执行JAR repackage | 通过 |
| JAR manifest及模块/迁移条目检查 | JarLauncher、Start-Class、推荐Service和V38均存在 |
| git diff --check | 通过 |

环境：Java21.0.5，Spring Boot4.1.1，PostgreSQL17.11，Redis7.4；独立数据库 `foodtime_hybridrec_v2_it`，全量测试使用Redis DB15。现有业务数据库未用于测试。

默认 `-Pintegration verify` 首次已通过358项单元测试，但Windows上的 `target/foodtime-neo-backend.jar` 被其他进程占用，repackage重命名失败。通过独占文件访问检查确认是占用，没有终止用户进程。

最终使用临时POM（只将finalName改为 `foodtime-neo-backend-hybridrec-verification`，其余与原pom逐字相同）执行完整 `-B -ntp -Pintegration verify`，05:28完成，BUILD SUCCESS；临时POM已经删除，正式pom.xml未改动。这次实际执行了完整repackage，没有跳过插件。

产物：`target/foodtime-neo-backend-hybridrec-verification.jar`，约65.7MB。完整日志：`target/hybridrec-full-verification.log`；XML结果位于 `target/surefire-reports` 和 `target/failsafe-reports`。这些构建产物按既有规则不入Git。

首次集成测试暴露测试夹具未隔离Flyway种子菜谱、图片夹具漏填上传人；修正后定向及全量全部通过。此前浮点精确断言也已改用误差容忍。没有删除既有测试或改变依赖以通过检查。

### 需求与证据对应

| 文档测试项 | 实际覆盖 |
|---|---|
| 1～3、29 贝叶斯与有限数值 | AlgorithmTest 的先验、极端单评、收敛及权重边界 |
| 4～5 热度与曝光 | 零/最大热度、E的0次/4次，IT历史计数 |
| 6～7 哈希 | 同日复现、日期/版本差异、5000个样本严格小于1 |
| 8～11 动态权重 | 冷/成熟/中间及1001种coverage权重和 |
| 12～15 冷启动、口碑配额、双池 | 零评1200菜、90天模拟、配额变化与重叠候选去重 |
| 16～18 冷却与放宽 | D-30/D-31边界、充足候选排除冷却、最老回填、先放宽多样性 |
| 19～21 档口/食堂约束 | 默认上限与覆盖、仅单档口时仍能返回10道 |
| 22～23 幂等与并发 | 真实数据库重复调用、6个独立实例共同生成 |
| 24 Redis降级 | 缓存读写抛错时仍能生成并读取数据库结果 |
| 25、27 下架/删除评论 | dish/stall/hall过滤、图片撤审、待审排除、评分更新及软删 |
| 26 全站统一 | 两个不同登录用户获得相同data；请求userId不影响结果 |
| 28 空候选 | 纯算法与真实数据库合法空快照 |
| 30 失败重试 | 第二条明细写入故障后全部回滚、failed不可读、下一次请求成功；调度可再次触发 |
| 提交后缓存、历史来源 | 独立连接确认提交后再缓存；失败明细不计历史；最近日期独立于90天窗口 |

## 7. 90天模拟实际统计

1200菜，75%零评论、20%有1～3条、4%有5～10条、1%有30～49条；6食堂、36档口。直接复用生产Generator，每日维护真实历史和冷却，2026-01-01至03-31连续90天：

- 每天10道不同菜，合计900位置。
- 30天内重复0，重复率0%。
- 不同菜品900道，覆盖率75%（本场景900/1200的理论最大值）。
- 食堂覆盖最少4，平均5.3667。
- 档口覆盖最少10，平均10。
- 无评论菜占比66.7778%。
- QUALITY=0%、EXPLORE=100%；因为成熟度5%对应口碑配额0，有评论低曝光菜仍可进入探索并参与综合评分。
- 次数分布：300道0次、900道1次。

[机器可读摘要](daily-recommendation-simulation.json)；完整逐日ID与指标由测试生成到 `target/recommendation-simulation.json`。固定模拟没有线上流量含义，不能推广为所有菜谱的覆盖率保证。

## 8. 性能瓶颈与未执行项

对1000～2000菜品使用少量批量SQL、内存贪心和最多10条明细写入。最近评论聚合、图片查找及全历史最近日期统计在更大规模时可能成为瓶颈，应以EXPLAIN和真实负载决定额外索引或汇总。

没有已知阻塞本次功能交付的代码问题。未做生产部署、线上吞吐压测，也未等待实际午夜00:05；调度使用注解及触发/失败重试测试验证。默认Windows产物文件的外部占用仍存在，使用正常未占用目录构建不需要临时POM。

本次没有前端页面、个性化、真实曝光埋点或客户端联调，这些不在任务范围。模拟是离线算法验收，未宣称线上推荐效果。

## 9. 本地运行与人工验收

1. 使用Java21和仓库Maven Wrapper，按 [配置说明](../ops/configuration.md) 准备PostgreSQL/Redis。构建/测试用独立测试库和Redis DB；测试会运行Flyway种子导入。
2. 设置 `DB_URL=jdbc:postgresql://127.0.0.1:5432/<测试库>?currentSchema=foodtime`、正确DB凭据和Redis配置；运行 `./mvnw.cmd -B -ntp -Pintegration verify`。Windows若产物被运行中的服务占用，先使用不冲突的构建目录/产物名，或在获准后停止对应服务。
3. 仅复跑算法与模拟：`./mvnw.cmd "-Dtest=RecommendationAlgorithmTest,RecommendationSimulationTest,DailyRecommendationCacheTest" test`，检查simulation.json中的逐日ID和汇总。
4. 使用正常开发库运行 `./mvnw.cmd spring-boot:run`（Flyway自动应用V38）。按 [auth.http](../api/auth.http) 获取会话，GET推荐接口。换另一个用户读取，对比data中的菜品ID与顺序。
5. 检查当天批次success、配置摘要长度64、明细位置从1开始且dish/position不重复。重复请求/重复生成不增加当天批次或历史次数。
6. 在测试库将入选菜品下架或所属档口停用，再读取确认已过滤；将图片改为pending/rejected确认不显示。恢复测试数据。不要手改成功推荐历史凑齐数量。
7. Redis断开降级、并发生成和部分写入故障请优先复跑DailyRecommendationIT，避免手工故障注入影响常用开发服务。
