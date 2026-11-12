# 已有目录纠错

首批支持食堂、档口、菜品的名称、文字介绍；菜品额外支持价格。纠错不改所属食堂／档口，不把证据图片加入目录图集。

## 权限与额度

| 动作 | 能力 | 默认角色 |
| --- | --- | --- |
| 提交、查看本人提案 | `correction.submit` | 所有认证用户 |
| 审核队列、详情、通过／拒绝 | `correction.review` | 仅超管 |

普通管理员保留原有目录直接编辑能力，但不会因为管理员身份获得 `correction.review`。审核权限通过集中能力策略判断，未来可替换策略下放。visitor 不能提交。

每个提交者按 UTC+8 自然日最多成功创建 5 单；每单 1–3 个字段，最多 3 张本人上传的证据。相同 `idempotencyKey` 重试不重复占额度；同一键用于不同内容返回 409。

## 提交提案

`POST /api/v1/users/me/catalog-corrections`

需要登录 Cookie 和 CSRF。示例：

```json
{
  "targetType": "dish",
  "targetId": "cc77444e-4ca7-42b6-89ba-e322e87667dc",
  "baseRevision": 1,
  "changedFields": ["name", "price"],
  "name": "红烧肉盖饭",
  "price": 15.50,
  "reason": "菜单已更新，实际价格为15.5元",
  "evidenceUrls": ["/api/v1/media/2f7b..."],
  "idempotencyKey": "1f250a9d-6dc8-4e5f-9ac1-4d7e2a5b0c11"
}
```

- `targetType`：`dining_hall`、`stall`、`dish`。
- `baseRevision`：提交前从目标详情读取的 `revision`；与当前版本不一致时返回 409，不静默覆盖。
- `changedFields`：字段操作列表，必须显式列出 `name`、`description`、`price` 中被修改的字段。`price` 仅菜品可用；价格 `null` 表示改为“价格未知”，`0` 是有效价格，两者与“未修改价格”不同。
- `description` 允许 `null` 或空串表示清空介绍。
- 未在 `changedFields` 中的字段即使传值也不会应用。
- `evidenceUrls` 必须来自本人通过 `POST /api/v1/uploads/images` 上传的暂存引用；证据保持私有，不公开缓存，也不会自动进入目录图集。
- `idempotencyKey` 长度不超过 64；建议客户端生成 UUID。

成功返回 201 和提案详情。目标不存在、已停用或当前用户不可见时返回 404，避免泄露资源存在性。

## 我的提案

`GET /api/v1/users/me/catalog-corrections?page=1&size=20`

只返回本人提案，最新在前。`GET /api/v1/users/me/catalog-corrections/{id}` 返回本人单条详情与证据引用；读取他人提案返回 404。

返回字段包含：`id`、`targetType`、`targetId`、提交时的 `targetSummary`、`baseRevision`、`originalName`、`originalDescription`、`originalPrice`、`proposedName`、`proposedDescription`、`proposedPrice`、`changedFields`、`reason`、`status`、`reviewNote`、`createdAt`、`reviewedAt`、`revision`、`evidenceUrls`。原值快照用于目录改名或删除后仍能理解提案，不作为长期越权展示目录内容的依据。

## 审核队列

`GET /api/v1/admin/catalog-corrections?status=pending&targetType=dish&page=1&size=20`

需要 `correction.review`。`status` 可为 `pending`、`approved`、`rejected`；`targetType` 可选。列表项外层为 `{ correction, submitterId, submitterName, submitterEmail }`；详情额外返回证据引用。

`POST /api/v1/admin/catalog-corrections/{id}/decision`

```json
{
  "decision": "approved",
  "reviewNote": "已核对菜单和价格",
  "expectedRevision": 1
}
```

整单通过或拒绝，不支持逐字段裁决。`expectedRevision` 是提案自身的 revision。通过时同一事务中：

1. 锁定提案并校验仍为 `pending`、revision 一致；
2. 锁定目标目录并校验 `baseRevision` 仍等于当前目录 revision、目标仍启用；
3. 条件更新批准字段并递增目录 revision；
4. 写入提案终态和改前／改后事件。

任一步冲突（目标删除、停用、已被直接编辑、并发审核）都会整体回滚并返回 409，提案保持 `pending`，不会出现“已通过但未应用”的半成功。拒绝只更新提案终态和审核记录，不修改目录。

## 错误码

| HTTP | code | 含义 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 字段、长度、价格、附件数量或目标类型不合法 |
| 401 | `UNAUTHENTICATED` | 未登录或会话失效 |
| 403 | `ACCESS_DENIED` | 无 `correction.submit`／`correction.review` |
| 404 | `CORRECTION_NOT_FOUND` | 提案不存在或不属于当前用户 |
| 404 | `CATALOG_NOT_FOUND` | 目标不存在或不可见 |
| 409 | `CATALOG_REVISION_CONFLICT` | 目录版本已变化，需刷新后重新核对 |
| 409 | `CATALOG_TARGET_GONE` / `CATALOG_TARGET_UNAVAILABLE` | 目标已删除／停用，不能应用 |
| 409 | `IDEMPOTENCY_KEY_REUSED` | 幂等键已用于不同内容 |
| 409 | `CORRECTION_ALREADY_DECIDED` | 提案已处理 |
| 429 | `CORRECTION_DAILY_LIMIT` | 超过 UTC+8 当日 5 单 |

## 实现与迁移

- `src/main/resources/db/migration/V49__catalog_corrections.sql`：目录 `revision` 列、提案、证据、事件表。
- `correction/CatalogCorrectionService`、`CatalogCorrectionStore`、`CatalogCorrectionController`、`CatalogCorrectionAdminController`。
- 目录详情响应新增 `revision`；管理员目录响应同样返回 `revision`。管理员直接编辑和纠错批准都会递增它。
- 证据读取由 `CorrectionEvidenceAccess` 限制为上传者本人和 `correction.review` 持有者；暂存清理会保留仍被提案引用的图片。
- 事件表与业务同事务写入；通知投递将由后续可靠 outbox 接入，投递失败不回滚审核结果。
