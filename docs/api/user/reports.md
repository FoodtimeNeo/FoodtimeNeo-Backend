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

## 追问

`GET /api/v1/users/me/reports/{id}/follow-ups?page=1&size=20`

仅举报人本人可读，返回公开追问／管理员回复和证据引用；内部备注不返回。

`POST /api/v1/users/me/reports/{id}/follow-ups`

```json
{
  "content": "请再核对一下菜单价格",
  "evidenceUrls": ["/api/v1/media/..."],
  "clientRequestId": "6d6f..."
}
```

- 仅举报人；举报处理中或已完成均可追加。
- 每账号 UTC+8 自然日跨案件最多 5 次；每次 1–500 字、最多 3 张本人上传的证据。
- `clientRequestId` 幂等；相同编号不同内容返回 409。
- 管理员公开回复使用 `/api/v1/admin/reports/{id}/follow-ups`，`internalNote` 仅管理端可见；管理员回复不占用户额度。

## 双方申诉

`POST /api/v1/users/me/moderation-decisions/{decisionId}/appeals`

```json
{
  "content": "复核理由，最多2000字",
  "evidenceUrls": ["/api/v1/media/..."],
  "clientRequestId": "6d6f..."
}
```

- 只有决定接收方本人可提交；举报人、被举报人各自最多一次，`side` 由服务器从决定接收关系确定。
- 必须在 `appealDeadline` 之前提交；截止时刻本身及之后返回 409 `APPEAL_DEADLINE_EXPIRED`。
- 每次最多 2000 字、3 张本人证据；`(decision_id, appellant_id, side)` 唯一，并发重复提交返回 409。
- `GET /api/v1/users/me/appeals`、`/{id}` 仅返回本人申诉；`GET /api/v1/users/me/moderation-decisions` 的 `appealStatus`／`appealOutcome` 展示本人申诉状态。

管理端：

| 方法 | 路径 | 语义 |
|---|---|---|
| GET | `/api/v1/admin/appeals?status=submitted` | 申诉队列，需要 `appeal.review` |
| GET | `/api/v1/admin/appeals/{id}` | 审核详情，含内部备注与证据 |
| POST | `/api/v1/admin/appeals/{id}/start?expectedRevision=1` | `submitted` → `under_review` |
| POST | `/api/v1/admin/appeals/{id}/resolve` | 保存复核结论并通知双方 |

复核结论 `outcome` 为 `upheld`（申诉成立）或 `rejected`（不成立），保存公开理由与内部备注；历史不会被覆盖，同一决定不再生成第二次申诉。裁决与实际恢复动作分离，本阶段不自动恢复处罚或下架。

## 证据访问

举报追问与申诉证据写入 `report_evidence`，只有上传者本人和具备 `report.review`／`appeal.review` 的审核者能通过媒体接口读取；普通管理员不能借管理身份读取他人申诉证据。暂存清理会保留被追问／申诉引用的图片。
