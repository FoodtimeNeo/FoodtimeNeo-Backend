# 平台数据看板（admin dashboard）

`GET /api/v1/admin/dashboard?days=7`。**仅超管（superadmin 且 active）可读**，普通管理员调同样 403。

## 口径定义（前后端统一口径）

- 业务时区：`Asia/Shanghai`。趋势按自然日切分，含当日（截至请求时刻）。
- `days`：1 到 31 的整数，默认 7；超出范围返回 400 `VALIDATION_ERROR`。
- `registeredUsers`：`users.status = 'active'` 的累计数。
- `totalUsers`：全部已注册账号数，包含停用账号，与 `registeredUsers` 分别返回。
- `trend[].totalUsers`：截至该业务日的累计注册账号数，包含停用账号。先统计窗口开始前的注册数，再逐日累计 `newUsers`；无新增日期保持前一日总量。停用账号不抹去历史注册记录。
- `onlineUsers`：最近 `DASHBOARD_PRESENCE_TTL`（默认 `PT5M`）内有过已认证请求的去重用户数。
  每次已认证请求写 `foodtime:presence:user:<userId>` 并自动过期；同一账号多端登录只算一次。
  与登录会话解耦：会话可能保留数天，但不再计入“在线”。
- `reviews` / `replies`：正式表全量（`dish_reviews` / `dish_review_replies`，只收录已审核内容）。
- `posts`：已发布社区动态数（`status='approved' AND deleted_at IS NULL`）。
- `halls` / `stalls` / `dishes`：`status='active'` 的累计数。
- `newUsers` / `newReviews` / `newReplies` / `newPosts`：当日 00:00（含）到次日 00:00（不含）的新增行数。
- `onlinePeak`：**当日去重活跃用户数**——当天在注册、点评投稿、回复、动态、评论、各类点赞任一表中出现过 `user_id` 的去重人数。
  说明：这是"日活"口径，不是并发连接峰值。历史日期天然可回算，上线第一天就有完整趋势。
- `trend`：按日正序，最早一天在前；无数据的日期补零（`new*` 与 `onlinePeak` 均为 0）。
- `generatedAt`：快照生成时间（UTC ISO-8601）。前端缓存/轮询以它判断新鲜度，不要用客户端时间推算。
- 聚合使用只读 `REPEATABLE_READ` 事务，总量、窗口前基数、每日新增和日活取自同一数据库快照，避免注册并发造成总量与趋势不一致。

## 示例

```bash
curl -b cookies.txt 'https://bjtufoodtime.top/api/v1/admin/dashboard?days=7'
# {"code":"OK","data":{"registeredUsers":...,"onlineUsers":...,"trend":[{"day":"2026-10-26",...}],...}}
```

## 运维说明

- Redis 不可用或聚合查询失败时返回 503 `DASHBOARD_UNAVAILABLE`（与写链路限流失败保持一致），不返回残缺数据。
- 该接口只读，不写库；无 Flyway 迁移。

## 2026-11-03 用户总量与统计周期

新增 `totalUsers` 与 `trend[].totalUsers`，原字段、权限与 `days=1..31` 保持兼容。Web 一次请求 `days=30`，默认展示末 7 天，可切换 30 天；新增汇总同时展示末 1、7、30 天的 `newUsers/newReviews/newReplies/newPosts` 之和。累计和最近 5 分钟在线数不随窗口切换。旧 Android 客户端忽略新增 JSON 字段，继续使用原有 7 日接口。
