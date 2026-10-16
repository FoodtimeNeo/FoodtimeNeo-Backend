# 我的菜品提交

`GET /api/v1/users/me/dish-submissions?page=1&size=20`

需要登录 Cookie，不需要 CSRF。`page` 默认1且不得小于1，`size` 默认20且范围为1到100。第一版返回全部审核状态，不提供状态筛选。

## 数据库设计与身份边界

数据来自 [dish_submissions](../../database-design.md#dish_submissions菜品提交审核表)。用户ID只取自校验后的会话，按 `submitted_by_user_id` 查询；传入 `userId` 或 `submittedByUserId` 不会改变所有者，管理员和超级管理员也只查看本人记录。单条SQL在同一快照读取本人总数和分页项。

名称、简介、参考价格和图片引用来自提交表，反映提交记录内容，不用正式菜品当前内容替换。档口和食堂ID及当前名称来自关联表；食堂通过档口关联，不假定提交表有食堂列。个人历史保留已停用食堂和档口的记录。

`price` 在待审核或拒绝状态可以为null，不能替换为0；非空值用 `BigDecimal` 对应 `NUMERIC(10,2)`。通过状态必须有价格及 `approved_dish_id`；其余状态的正式关联为空。非待审核记录必须有审核人和时间，这些已有约束由数据库保证。查询不创建正式菜品或图片，也不修改状态、不执行审核、不写审核日志。

## 响应

统一响应的 `data` 为 `{items,total,page,size}`；按 `created_at DESC, id ASC` 稳定排序。无本人记录或超出末页时 `items=[]`，总数保留。分页、排序、错误码及公开字段选择属于接口约定。

| 每项字段 | 来源 / 含义 |
| --- | --- |
| `id` | 菜品提交UUID |
| `stallId`、`stallName`、`diningHallId`、`diningHallName` | 所属档口及食堂 |
| `name`、`description`、`price`、`imageUrl` | 已提交内容；简介及参考价格允许null |
| `status` | `pending` / `approved` / `rejected` |
| `reviewedAt`、`reviewReason` | 审核时间及说明，允许null |
| `approvedDishId` | 通过后正式菜品UUID，其余状态为null |
| `createdAt`、`updatedAt` | 提交记录创建及更新时间，UTC时间字符串 |

不返回审核人员身份、邮箱或密码。关联正式菜品已停用时，个人提交仍保留该关联；公开菜品详情继续遵守其展示状态过滤。

| HTTP | code | 含义 |
| --- | --- | --- |
| 400 | `BAD_REQUEST` / `VALIDATION_ERROR` | 分页解析失败或超出范围 |
| 401 | `UNAUTHENTICATED` | 未登录、会话失效或账号禁用 |
| 503 | `DISH_SUBMISSIONS_UNAVAILABLE` | 查询不可用 |
| 503 | `AUTH_UNAVAILABLE` | 认证基础设施不可用 |

实现为 `api/user/MyDishSubmissionController` 与 `dish` 领域的DTO、投影、Mapper和Service。无数据库迁移、无业务写入。
