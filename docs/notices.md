# 公告与消息（notices / messages）

供 Web / 安卓端照着调。后端分支 `feature/notice-inbox-backend`，迁移 `V26__notices_messages.sql`。

## 表结构

### foodtime.notices（全员广播）

| 列 | 类型 | 说明 |
|---|---|---|
| id | UUID PK | 公告主键 |
| title / body | TEXT NOT NULL | 标题 / 正文 |
| seq | BIGSERIAL UNIQUE | 单调递增轮询游标，唯一，仅作增量游标，不对外当身份 |
| published_at | TIMESTAMPTZ，默认 now() | 发布时间 |
| created_by | UUID → users | 发布人 |

### foodtime.notice_reads（公告已读）

联合主键 `(notice_id, user_id)`，`notice_id` 级联删除；索引 `ix_notice_reads_user(user_id, notice_id)`。
`read_at` 默认 now()。

### foodtime.messages（个人收件箱）

| 列 | 类型 | 说明 |
|---|---|---|
| id | UUID PK | 消息主键 |
| user_id | UUID → users | 收件用户 |
| title / body | TEXT NOT NULL | 标题 / 正文 |
| category | TEXT，默认 SYSTEM | 消息分类 |
| ref_type / ref_id | TEXT / UUID，可空 | 关联业务类型 / ID |
| is_read | BOOLEAN，默认 FALSE | 是否已读 |
| created_at | TIMESTAMPTZ，默认 now() | 创建时间 |

索引：`ix_messages_user(user_id, id)`，部分索引 `ix_messages_user_unread(user_id) WHERE NOT is_read`。

## 接口（全部 Session Cookie 鉴权）

### 用户端

- `GET /api/v1/users/me/notices/feed?since_seq=0&limit=20`
  → `{ items: [{ id, title, body, publishedAt }], unreadCount }`
- `GET /api/v1/users/me/notices?page&size` → 分页公告（倒序）
- `POST /api/v1/users/me/notices/{id}/read` → 200 空 data；公告不存在 404（`NOTICE_NOT_FOUND`）
- `GET /api/v1/users/me/messages?page&size` → 分页消息（倒序，含 `isRead`、`category`、`refType`、`refId`、`createdAt`）
- `GET /api/v1/users/me/messages/unread-count` → `{ unreadCount }`
- `POST /api/v1/users/me/messages/{id}/read` → 非本人消息按 404（`MESSAGE_NOT_FOUND`）处理，不泄露归属
- `POST /api/v1/users/me/messages/read-all` → 全部标已读

### 管理端（admin / superadmin 且 status=active，否则 403 `ACCESS_DENIED`）

- `GET /api/v1/admin/notices?page&size` → 分页列表
- `GET /api/v1/admin/notices/{id}` → 详情；不存在 404
- `POST /api/v1/admin/notices` 体 `{ title(≤200), body(≤20000) }` → 201 新公告
- `PUT /api/v1/admin/notices/{id}` 体同上 → 整体替换；不存在 404
- `DELETE /api/v1/admin/notices/{id}` → 硬删，已读记录级联删除；不存在 404

## 轮询增量 since_seq 语义

1. 首次 `since_seq=0` 全量拉取（`limit` 上限 100），记录返回 items 中的最大 seq（按发布顺序 items 已正序）。
2. 之后每次带上该最大值轮询，仅返回新公告；空数组表示无更新。
3. `seq` 由数据库 `BIGSERIAL` 递增，编辑公告不改变 `seq`（编辑不视为新公告，未读标记保留）。
4. `unreadCount` = 公告总数 − 该用户已读数；调用已读接口后重新拉取 feed 或用本地计数减一。

## 已读语义

- 公告已读：`POST .../notices/{id}/read` 幂等（`ON CONFLICT DO NOTHING`），重复调用返回成功。
- 消息已读：单条与全部均为幂等；已读消息仍保留在列表中，`isRead=true`。
- 小红点：公告用 feed 的 `unreadCount`，消息用 `/unread-count`；读操作后重新拉取。
