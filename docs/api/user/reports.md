# 举报回执、决定与申诉期限（第一阶段）

本阶段交付举报决定、双方站内回执、公开理由和 7 天申诉截止；追问与申诉提交在后续阶段接入。

## 权限

| 动作 | 能力 | 默认角色 |
|---|---|---|
| 查看本人举报、本人收到的决定 | 登录用户本人 | `user`、`admin`、`superadmin` |
| 提交举报 | 既有举报接口 | 登录用户；visitor 仍可举报可访问内容 |
| 作决定 | `report.review` | 仅 `superadmin` |

普通管理员没有 `report.review`；旧 `/admin/reports/{id}/review` 仅保留兼容，不产生公开决定。

## 我的举报

`GET /api/v1/users/me/reports?page=1&size=20`

只返回当前账号提交的举报，包含本人举报理由、目标摘要和公开结果。状态：

- `pending`：待处理；
- `completed`：已有公开决定；
- `legacy`：旧流程已标记处理但没有公开理由，不能伪造成立／不成立。

`GET /api/v1/users/me/reports/{id}` 仅本人可读；他人返回 404。

## 收到的决定

`GET /api/v1/users/me/moderation-decisions`

返回本人作为举报人或被举报人收到的决定。被举报人投影不包含举报人 UUID、举报理由原文、`reportId` 或内部备注。

```json
{
  "decisionId": "6d6f...",
  "reportId": "1a2b...",
  "side": "reporter",
  "targetType": "post",
  "outcome": "upheld",
  "publicReason": "经核对，内容违反社区规范",
  "deliveredAt": "2026-11-12T08:00:00Z",
  "appealDeadline": "2026-11-19T08:00:00Z"
}
```

- `appealDeadline` 是决定成功写入本人站内信箱时持久化的 `deliveredAt + 7×24 小时`；打开详情、推送失败或点击通知不会重算。
- `side=target` 的 `reportId` 为 null。
- `GET /users/me/moderation-decisions/{decisionId}` 仅决定接收方本人可读。

## 管理员决定

`POST /api/v1/admin/reports/{id}/decision`

```json
{
  "outcome": "upheld",
  "publicReason": "经核对，内容违反社区规范",
  "internalNote": "仅内部可见的处理备注"
}
```

- `outcome`：`upheld`（成立）／`not_upheld`（不成立）。
- `publicReason` 1–500 字；`internalNote` 最多 2000 字，仅管理端保存。
- 同一举报只能决定一次；并发审核或重复请求返回 409 `REPORT_ALREADY_DECIDED`。
- 同一事务内完成：决定、举报终态、双方站内消息与通知事件；通知投递失败不回滚决定，但事件/入箱投影与决定同一事务。
- 本阶段不自动下架内容或停用账号；真实管理动作需另走已有能力并关联 actionId（后续阶段）。

## 实现与迁移

- `V52__report_decisions.sql`：`content_reports.revision`、`report_decisions`、`report_decision_recipients`。
- `safety/ReportDecisionService`、`ReportDecisionStore`、`api/user/ReportReceiptController`、`api/admin/ReportsAdminController`。
- 决定通过 `NotificationPublisher` 发布 `report` 事件并写站内消息；七类开关中的 `report` 默认关闭，因此不产生 Android 系统提醒，但站内始终可读。
- 追问、申诉提交／复核、受限申诉会话和实际处罚动作在后续阶段实现。
