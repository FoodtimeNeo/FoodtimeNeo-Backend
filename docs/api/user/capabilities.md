# 当前有效能力

`GET /api/v1/users/me/capabilities`

需要有效会话 Cookie；只读，不需要 CSRF。

## 用途

服务器把当前账号的有效能力投影给 Web / Android。客户端只根据返回的能力显示或启用动作；每次写请求仍由服务器重新判断，客户端传来的能力名、角色或 `allowedActions` 不能作为授权凭据。

## 响应

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "capabilities": ["catalog.edit", "correction.submit", "moderation.review"],
    "revision": "default-v2"
  },
  "timestamp": "2026-11-12T00:00:00Z",
  "requestId": "capability-example-1"
}
```

| 字段 | 说明 |
| --- | --- |
| `capabilities` | 稳定能力名数组，按名称排序；空数组表示当前账号没有任何管理能力 |
| `revision` | 策略版本。变化时客户端应重新读取；旧页面/旧会话不能沿用新权限 |

## 当前默认映射

| 能力 | 说明 | 默认角色 |
| --- | --- | --- |
| `moderation.review` | 审核投稿、社区内容等既有管理动作 | `admin`、`superadmin` |
| `catalog.edit` | 目录编辑 | `admin`、`superadmin` |
| `feedback.manage` | 反馈回复、导入导出 | `superadmin` |
| `notice.manage` | 公告管理 | `superadmin` |
| `user.manage` | 用户角色/状态管理 | `superadmin` |
| `invitation.manage` | 邀请码发放与管理 | `superadmin` |
| `ai.model.manage` | AI 模型热切换 | `superadmin` |
| `correction.submit` | 提交目录纠错提案 | `user`、`admin`、`superadmin` |
| `correction.review` | 目录纠错审核 | `superadmin` |
| `report.review` | 举报处理（G2 预留） | `superadmin` |
| `report.reply` | 举报回复（G2 预留） | `superadmin` |
| `appeal.review` | 申诉复核（G2 预留） | `superadmin` |

普通管理员可以提交纠错，但不获得纠错审核、举报或申诉能力；当前 `admin` 仅有 `moderation.review`、`catalog.edit` 与 `correction.submit`。

## 缓存与失效

- 角色级能力快照按账号、角色、账号状态和策略版本缓存最多 60 秒。
- 角色/状态变化、策略版本变化或显式失效后，下一次读取得到新结果。
- 资源级判断（未来按食堂/范围授权）直接调用策略，不经过角色级缓存。

## 错误

| HTTP | `code` | 说明 |
| --- | --- | --- |
| 401 | `UNAUTHENTICATED` | 未登录或会话失效 |
| 500 | `INTERNAL_ERROR` | 未预期服务故障 |

## 实现位置

`authz/AuthorizationPolicy` 是可替换策略接口；`authz/DefaultAuthorizationPolicy` 是当前集中默认映射；`authz/CapabilityService` 负责能力缓存、资源级判断和快照；`api/user/CapabilityController` 只做身份上下文、DTO 投影与响应。业务代码不得再散落 `hasRole("superadmin")` 或角色字符串比较来决定新功能权限。
