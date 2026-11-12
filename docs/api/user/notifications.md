# 通知事件与提醒偏好（基础）

本组先建立可靠事件、站内消息投影、Android 交付记录和七类开关契约；完整七类业务接入与真机调度在 W4 继续。

## 七类开关

`GET /api/v1/users/me/notification-preferences`

```json
{
  "items": [
    { "category": "message", "enabled": true, "revision": 1 },
    { "category": "interaction", "enabled": false, "revision": 1 },
    { "category": "notice", "enabled": false, "revision": 1 },
    { "category": "feedback", "enabled": false, "revision": 1 },
    { "category": "correction", "enabled": false, "revision": 1 },
    { "category": "report", "enabled": false, "revision": 1 },
    { "category": "appeal", "enabled": false, "revision": 1 }
  ]
}
```

- 私信沿用旧开关语义，默认开启；其余六类首次默认关闭，不静默扩大提醒。
- 站内消息和未读始终可读，不受开关影响；开关只决定是否创建 Android 系统提醒交付记录。
- `PUT /api/v1/users/me/notification-preferences/{category}`，请求 `{ "enabled": true, "expectedRevision": 1 }`。revision 冲突返回 409。
- 开启时记录当前最大事件 seq 作为基线；开启前的事件不补发。关闭后重新开启会更新基线，关闭期间事件不会重放。

## 事件与游标

`GET /api/v1/users/me/notification-events?afterSeq=0&limit=50`

返回本人 `seq > afterSeq` 的事件，按 seq 升序，最多 100 条。`nextSeq` 是本次最后一条的 seq，为空时保留原游标。

```json
{
  "items": [
    {
      "eventId": "6d6f...",
      "seq": 12,
      "category": "correction",
      "title": "目录纠错已通过",
      "body": "你提交的目录纠错已通过并应用。",
      "refType": "catalog-correction",
      "refId": "cc77...",
      "refCommentId": null,
      "refRootCommentId": null,
      "createdAt": "2026-11-12T08:00:00Z",
      "deliveryStatus": "pending"
    }
  ],
  "nextSeq": 12
}
```

- `eventId` 是稳定业务事件 ID，客户端用它去重；不能用未读总数代替。
- `deliveryStatus` 为 `null` 表示该事件没有 Android 交付记录（对应开关关闭或基线之前）；`pending`、`delivered`、`failed` 表示投递状态。
- 事件创建即站内消息入箱；站内消息 `/users/me/messages` 与事件同事务写入。事件入箱时间 `createdAt` 可作为后续举报/申诉期限的可靠起算点。

`POST /api/v1/users/me/notification-events/{eventId}/delivered`

幂等标记 Android 交付完成，写入 `delivered_at`；只影响投递记录，不改变站内已读或事件入箱时间。没有本人交付记录时返回 409。

## 事件发布

业务模块在同一事务调用 `notification/NotificationPublisher.publish(...)`：

- 校验／初始化七类偏好；
- 写入 `notification_events`（稳定 eventId、seq）；
- 写入 `messages` 站内投影（`event_id` 唯一，重放不重复）；
- 若该类别开启且事件 seq 晚于启用基线，插入 `notification_deliveries` pending。

通知投递失败不回滚业务；本基础不启动无限后台轮询，W4 再接入 Android JobScheduler 与七类业务事件。

## 迁移与实现

- `V51__notification_foundation.sql`：事件、偏好、交付表；`messages.event_id` 唯一索引。
- `notification/NotificationPublisher`、`NotificationStore`、`NotificationService`、`api/user/NotificationController`。
- 目录纠错审核已通过 publisher 发布 `correction` 事件，站内消息可见；其余六类在 W4 接入。
