# 我的评论审核记录

`GET /api/v1/users/me/review-submissions?page=1&size=20`

需要登录 Cookie，不需要 CSRF。只接受分页约定：`page` 默认1且不得小于1，`size` 默认20且范围为1到100。查询返回本人所有审核状态，第一版不提供状态筛选。

## 设计依据与身份边界

记录来自 [dish_review_submissions](../../database-design.md#dish_review_submissions菜品评论审核表)，不是正式评论表。`pending`、`approved`、`rejected` 与1到5的整数评分沿用现有约束；待审核记录的审核时间及说明可以为null。正式评论创建、再次审核后更新正式评论，以及写入审核日志由后续审核接口负责，本接口不执行这些操作。

用户ID只取自服务端校验后的登录身份；请求没有 `userId` 参数，额外传入不能改变所有者。管理员和超级管理员同样只能查询自己。记录及总数使用同一所有者过滤，在单条SQL的同一快照读取。

关联菜品、档口、食堂的ID和当前名称来自已有外键关系，不新增名称快照列。个人历史不应用公开展示状态过滤，所属菜品、档口或食堂停用后仍可查看。关联数据受 `ON DELETE RESTRICT` 保护。

## 响应

统一响应的 `data` 为 `{items,total,page,size}`；按 `submitted_at DESC, id ASC` 排序。无本人记录或超出末页时 `items=[]`，总数仍是本人真实记录数。

| 每项字段 | 来源 / 含义 |
| --- | --- |
| `id`、`dishId` | 提交及菜品UUID |
| `dishName`、`stallId`、`stallName`、`diningHallId`、`diningHallName` | 关联菜品、档口与食堂 |
| `rating`、`content`、`status` | 评分、提交正文、审核状态 |
| `submittedAt`、`updatedAt` | 提交与更新时间，UTC时间字符串 |
| `reviewedAt`、`reviewReason` | 审核时间及说明，允许null |

不返回审核人员身份、用户邮箱或密码。分页、排序和公开字段选择属于接口约定。

| HTTP | code | 含义 |
| --- | --- | --- |
| 400 | `BAD_REQUEST` / `VALIDATION_ERROR` | 分页解析失败或超出范围 |
| 401 | `UNAUTHENTICATED` | 未登录、会话失效或账号禁用 |
| 503 | `REVIEW_SUBMISSIONS_UNAVAILABLE` | 查询不可用 |
| 503 | `AUTH_UNAVAILABLE` | 认证基础设施不可用 |

实现为 `api/user/MyReviewSubmissionController` 和 `review` 领域的DTO、查询投影、Mapper、Service。无数据库迁移，无业务写入。
