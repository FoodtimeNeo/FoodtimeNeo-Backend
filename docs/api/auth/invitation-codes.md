# 邀请码

注册可选归因：用户可在 [注册](register.md) 时填写平台发放的12位邀请码，服务端校验有效后将用户与该码关联（`users.invited_by_code_id`），不填不影响注册。发放与管理仅平台超级管理员可用。

## 设计依据与额外约定

[数据库设计](../../database-design.md#invitation_codes邀请码表) 仅提供邀请码与归因字段的持久化结构，没有规定邀请码格式、必填性、发放流程、复用次数或有效期。这些是本次新增的接口约定：注册可选填写、平台管理员发放、可复用，均按用户确认保留；是否改为必填或用户互邀属于待确认事项。`requestId`、响应 `timestamp` 仍是协议数据，不是业务表字段。

## 注册使用

注册请求中的 `invitationCode` 可空：缺失或空白视为未填写；非空时去除首尾空白并转大写后，须完全匹配12位发放字符（`A–H J–N P–Z 2–9`，去掉易混淆的 `0 O 1 I L`）。

- 有效（启用、未过期、次数未用尽）则在同一事务中关联用户并计一次使用，返回201。
- 无效、过期、停用或次数用尽返回400 `INVITATION_CODE_INVALID`，不创建账号。
- 未配置邀请码密钥（`INVITATION_SECRET` 少于32字符）时，填写邀请码的注册返回503 `INVITATION_UNAVAILABLE`；不填不受影响。

## 管理接口（前缀 `/api/v1/admin/invitation-codes`）

均需登录Cookie和CSRF令牌，且每次请求重读服务端角色，仅 `superadmin` 可用；普通管理员调用返回403。不提供删除接口，停用代替删除以保留审计；已用该码注册的账号不受停用影响。

| 功能 | 方法与路径 | 说明 |
| --- | --- | --- |
| 创建邀请码 | `POST /api/v1/admin/invitation-codes` | 成功201；请求 `{label?, maxUses?, expiresAt?}`，三项全可空 |
| 查看邀请码 | `GET /api/v1/admin/invitation-codes?status=&page=&size=` | 分页与总数，`status` 仅接受 `active/disabled` |
| 启用或停用 | `PUT /api/v1/admin/invitation-codes/{id}` | 请求 `{status}`，不存在返回404 |

创建成功示例：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "id": "9e55b67f-f051-4019-bafd-a4082c95f2a6",
    "code": "ABCD2345EFGH",
    "label": "2026迎新",
    "status": "active",
    "maxUses": 100,
    "expiresAt": "2026-12-31T16:00:00Z",
    "createdAt": "2026-10-17T08:00:00Z"
  },
  "timestamp": "2026-10-17T08:00:00Z",
  "requestId": "invitation-example-1"
}
```

**明文码只在本次响应中返回一次**，请妥善保存；服务端只存HMAC-SHA256摘要，列表与数据库中均无明文。`maxUses` 空表示不限次数；`expiresAt` 空表示长期有效；并发注册通过行锁串行计次，不会超发。配置密钥见 [配置说明](../../configuration.md)，本地变量见 `.env.example` 的 `INVITATION_SECRET`。

## 实现位置

`invitation/dto` 定义请求与响应；`invitation/mapper` 负责参数化SQL（含 `FOR UPDATE` 行锁）；`invitation/service` 编排生成/核销/管理与超管权限校验；`api/admin` 接收管理请求；`auth/service` 在注册事务中调用核销，失败整体回滚，不遗留已计次。
