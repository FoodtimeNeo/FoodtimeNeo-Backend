本文以用户提供的数据库设计为基础；2026-10-13 按后续要求移除 `stalls.floor`（V3）；2026-10-14 经确认补充图片审核信息与日志关联（V4）；2026-10-17 新增邀请码表与用户归因列（V7）、补充热点查询索引（V8，索引迁移，不改表结构）、新增AI预检结论表（V9，结论只读）。2026-10-18 评论通过保留投稿ID（V10）、AI自动审核身份列（V11）、社区动态全套表与审核日志社区列（V12）、AI结论类型放开社区（V13）。

# users（用户表）

| 字段 | PostgreSQL 类型 | 约束 / 默认值 | 简要说明 |
| --- | --- | --- | --- |
| `id` | `UUID` | `PRIMARY KEY` | 用户唯一标识，由后端生成。 |
| `email` | `VARCHAR(254)` | `NOT NULL CHECK (email = lower(btrim(email)))` | 注册和登录邮箱；保存为去除首尾空格后的小写形式。 |
| 唯一索引 | — | `UNIQUE (lower(btrim(email)))` | 邮箱不区分大小写且去除首尾空格后唯一。 |
| `password_hash` | `TEXT` | `NOT NULL` | 密码哈希值，不存储明文密码。 |
| `email_verified_at` | `TIMESTAMPTZ` | 可空 | 邮箱验证时间。 |
| `status` | `VARCHAR(20)` | `NOT NULL DEFAULT 'active'`；`active` / `disabled` | 账号状态。 |
| `role` | `VARCHAR(20)` | `NOT NULL DEFAULT 'user' CHECK (role IN ('user', 'admin', 'superadmin'))` | 用户角色；权限由服务端校验，用户不能自行修改。 |
| `password_changed_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 最近一次密码设置或修改时间。 |
| `last_login_at` | `TIMESTAMPTZ` | 可空 | 最近一次成功登录时间。 |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 记录创建时间。 |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 记录最后更新时间。 |
| `display_name` | `VARCHAR(50)` | `NOT NULL` | 用户昵称；未设置时由系统生成。 |
| `avatar_url` | `TEXT` | 可空 | 用户头像地址或存储引用。 |
| `invited_by_code_id` | `UUID` | 可空，`REFERENCES invitation_codes(id) ON DELETE RESTRICT` | 注册时使用的邀请码，仅记录归因关系。 |

# dining_halls（食堂表）

| 字段 | PostgreSQL 类型 | 约束 / 默认值 | 简要说明 |
| --- | --- | --- | --- |
| `id` | `UUID` | `PRIMARY KEY` | 食堂唯一标识，由后端生成。 |
| `name` | `VARCHAR(100)` | `NOT NULL` | 食堂名称。 |
| `cover_image_url` | `TEXT` | 可空 | 封面图片地址或存储引用。 |
| `description` | `TEXT` | 可空 | 食堂简介。 |
| `status` | `VARCHAR(20)` | `NOT NULL DEFAULT 'active'`；`active` / `inactive` | 展示状态；`inactive` 时不向学生展示。 |
| `latitude` | `NUMERIC(10,7)` | 可空；与 `longitude` 同时填写或同时为空 | 食堂纬度，用于距离计算。 |
| `longitude` | `NUMERIC(10,7)` | 可空；与 `latitude` 同时填写或同时为空 | 食堂经度，用于距离计算。 |
| `sort_order` | `INTEGER` | `NOT NULL DEFAULT 0` | 食堂列表中的排序值。 |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 记录创建时间。 |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 记录最后更新时间。 |

# stalls（档口表）

| 字段 | PostgreSQL 类型 | 约束 / 默认值 | 简要说明 |
| --- | --- | --- | --- |
| `id` | `UUID` | `PRIMARY KEY` | 档口唯一标识，由后端生成。 |
| `dining_hall_id` | `UUID` | `NOT NULL REFERENCES dining_halls(id) ON DELETE RESTRICT` | 所属食堂 ID。 |
| `name` | `VARCHAR(100)` | `NOT NULL`；`UNIQUE (dining_hall_id, name)` | 档口名称。 |
| `description` | `TEXT` | 可空 | 档口简介。 |
| `cover_image_url` | `TEXT` | 可空 | 封面图片地址或存储引用。 |
| `status` | `VARCHAR(20)` | `NOT NULL DEFAULT 'active'`；`active` / `inactive` | 展示状态；`inactive` 时不向学生展示。 |
| `sort_order` | `INTEGER` | `NOT NULL DEFAULT 0` | 所属食堂内的排序值。 |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 记录创建时间。 |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 记录最后更新时间。 |

# dishes（菜品表）

| 字段 | PostgreSQL 类型 | 约束 / 默认值 | 简要说明 |
| --- | --- | --- | --- |
| `id` | `UUID` | `PRIMARY KEY` | 菜品唯一标识，由后端生成。 |
| `stall_id` | `UUID` | `NOT NULL REFERENCES stalls(id) ON DELETE RESTRICT` | 所属档口 ID。 |
| `name` | `VARCHAR(120)` | `NOT NULL` | 菜品名称。 |
| `description` | `TEXT` | 可空 | 菜品简介。 |
| `price` | `NUMERIC(10,2)` | `NOT NULL CHECK (price >= 0)` | 菜品当前价格。 |
| `status` | `VARCHAR(20)` | `NOT NULL DEFAULT 'active'`；`active` / `inactive` | 展示状态；`inactive` 时不向学生展示。 |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 记录创建时间。 |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 记录最后更新时间。 |

# dish_images（菜品图片表）

| 字段 | PostgreSQL 类型 | 约束 / 默认值 | 简要说明 |
| --- | --- | --- | --- |
| `id` | `UUID` | `PRIMARY KEY` | 图片记录唯一标识，由后端生成。 |
| `dish_id` | `UUID` | `NOT NULL REFERENCES dishes(id) ON DELETE RESTRICT` | 所属菜品 ID。 |
| `uploaded_by_user_id` | `UUID` | `NOT NULL REFERENCES users(id) ON DELETE RESTRICT` | 上传用户 ID。 |
| `image_url` | `TEXT` | `NOT NULL` | 图片地址或存储引用。 |
| `status` | `VARCHAR(20)` | `NOT NULL DEFAULT 'pending'`；`pending` / `approved` / `rejected` | 图片审核状态。 |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 图片记录创建时间。 |
| `reviewed_by_user_id` | `UUID` | 可空；`REFERENCES users(id) ON DELETE RESTRICT` | 图片审核人；2026-10-14 经项目负责人确认由 V4 新增。 |
| `reviewed_at` | `TIMESTAMPTZ` | 可空 | 图片审核时间；旧记录不补造历史数据。 |
| `review_reason` | `TEXT` | 可空 | 图片审核说明或驳回原因。 |

# dish_review_submissions（菜品评论审核表）

| 字段 | PostgreSQL 类型 | 约束 / 默认值 | 简要说明 |
| --- | --- | --- | --- |
| `id` | `UUID` | `PRIMARY KEY` | 评论提交唯一标识，由后端生成。 |
| `dish_id` | `UUID` | `NOT NULL REFERENCES dishes(id) ON DELETE RESTRICT` | 被评论菜品 ID。 |
| `user_id` | `UUID` | `NOT NULL REFERENCES users(id) ON DELETE RESTRICT` | 评论提交用户 ID。 |
| `rating` | `SMALLINT` | `NOT NULL CHECK (rating BETWEEN 1 AND 5)` | 评论评分，范围为 1 至 5 分。 |
| `content` | `TEXT` | `NOT NULL` | 待审核的评论正文。 |
| `status` | `VARCHAR(20)` | `NOT NULL DEFAULT 'pending'`；`pending` / `approved` / `rejected` | 提交审核状态；通过后写入正式评论表 `dish_reviews`。 |
| `submitted_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 评论提交时间。 |
| `reviewed_by_user_id` | `UUID` | 可空；`REFERENCES users(id) ON DELETE RESTRICT` | 审核用户 ID。 |
| `reviewed_at` | `TIMESTAMPTZ` | 可空 | 审核完成时间。 |
| `review_reason` | `TEXT` | 可空 | 审核说明或拒绝原因。 |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 记录最后更新时间。 |
| 组合约束 | — | 同一 `(dish_id, user_id)` 最多一条 `pending` 记录；非待审核时必须填写审核人和审核时间 | 保留历史提交并避免重复待审核。 |

# dish_reviews（菜品评论表）

| 字段 | PostgreSQL 类型 | 约束 / 默认值 | 简要说明 |
| --- | --- | --- | --- |
| `id` | `UUID` | `PRIMARY KEY` | 正式评论唯一标识，由后端生成。 |
| `dish_id` | `UUID` | `NOT NULL REFERENCES dishes(id) ON DELETE RESTRICT` | 被评论菜品 ID。 |
| `user_id` | `UUID` | `NOT NULL REFERENCES users(id) ON DELETE RESTRICT` | 评论发布用户 ID。 |
| `rating` | `SMALLINT` | `NOT NULL CHECK (rating BETWEEN 1 AND 5)` | 评论评分，范围为 1 至 5 分。 |
| `content` | `TEXT` | `NOT NULL` | 已通过审核的评论正文。 |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 评论提交时间；通过审核后从审核提交记录复制。 |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 记录最后更新时间。 |
| 组合约束 | — | `UNIQUE (dish_id, user_id)` | 同一用户对同一菜品只保留一条正式评论；再次提交并审核通过时更新原记录。 |

# dish_rating_summary（物化视图；每 24 小时刷新）

| 字段 | PostgreSQL 类型 | 计算方式 | 简要说明 |
| --- | --- | --- | --- |
| `dish_id` | `UUID` | 来自 `dishes.id`；`UNIQUE INDEX` | 菜品 ID；唯一索引用于支持并发刷新。 |
| `average_score` | `NUMERIC(3,2)` | 正式评论的 `AVG(rating)`；无评论时为空 | 菜品平均评分。 |
| `review_count` | `BIGINT` | 正式评论的数量 | 菜品正式评论总数。 |

# stall_submissions（档口提交审核表）

| 字段 | PostgreSQL 类型 | 约束 / 默认值 | 简要说明 |
| --- | --- | --- | --- |
| `id` | `UUID` | `PRIMARY KEY` | 档口提交唯一标识，由后端生成。 |
| `dining_hall_id` | `UUID` | `NOT NULL REFERENCES dining_halls(id) ON DELETE RESTRICT` | 所属食堂 ID。 |
| `name` | `VARCHAR(100)` | `NOT NULL` | 提交的档口名称。 |
| `description` | `TEXT` | 可空 | 提交的档口简介。 |
| `cover_image_url` | `TEXT` | `NOT NULL` | 封面图片地址或存储引用；通过后写入 `stalls.cover_image_url`。 |
| `submitted_by_user_id` | `UUID` | `NOT NULL REFERENCES users(id) ON DELETE RESTRICT` | 提交用户 ID。 |
| `status` | `VARCHAR(20)` | `NOT NULL DEFAULT 'pending'`；`pending` / `approved` / `rejected` | 提交审核状态。 |
| `reviewed_by_user_id` | `UUID` | 可空；`REFERENCES users(id) ON DELETE RESTRICT` | 审核用户 ID。 |
| `reviewed_at` | `TIMESTAMPTZ` | 可空 | 审核完成时间。 |
| `review_reason` | `TEXT` | 可空 | 审核说明或拒绝原因。 |
| `approved_stall_id` | `UUID` | 可空；`UNIQUE REFERENCES stalls(id) ON DELETE RESTRICT` | 审核通过后创建的正式档口 ID。 |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 提交时间。 |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 记录最后更新时间。 |
| 组合约束 | — | `CHECK ((status = 'approved') = (approved_stall_id IS NOT NULL))`；`CHECK (status = 'pending' OR (reviewed_by_user_id IS NOT NULL AND reviewed_at IS NOT NULL))` | 保证审核状态、审核信息与正式档口关联一致。 |

# dish_submissions（菜品提交审核表）

| 字段 | PostgreSQL 类型 | 约束 / 默认值 | 简要说明 |
| --- | --- | --- | --- |
| `id` | `UUID` | `PRIMARY KEY` | 菜品提交唯一标识，由后端生成。 |
| `stall_id` | `UUID` | `NOT NULL REFERENCES stalls(id) ON DELETE RESTRICT` | 所属档口 ID；所属食堂由档口关联取得。 |
| `name` | `VARCHAR(120)` | `NOT NULL` | 提交的菜品名称。 |
| `description` | `TEXT` | 可空 | 提交的菜品简介。 |
| `price` | `NUMERIC(10,2)` | 可空；`CHECK (price >= 0)` | 提交的参考价格；通过审核前须补齐并写入 `dishes.price`。 |
| `image_url` | `TEXT` | `NOT NULL` | 图片地址或存储引用；通过后写入 `dish_images.image_url`。 |
| `submitted_by_user_id` | `UUID` | `NOT NULL REFERENCES users(id) ON DELETE RESTRICT` | 提交用户 ID。 |
| `status` | `VARCHAR(20)` | `NOT NULL DEFAULT 'pending'`；`pending` / `approved` / `rejected` | 提交审核状态。 |
| `reviewed_by_user_id` | `UUID` | 可空；`REFERENCES users(id) ON DELETE RESTRICT` | 审核用户 ID。 |
| `reviewed_at` | `TIMESTAMPTZ` | 可空 | 审核完成时间。 |
| `review_reason` | `TEXT` | 可空 | 审核说明或拒绝原因。 |
| `approved_dish_id` | `UUID` | 可空；`UNIQUE REFERENCES dishes(id) ON DELETE RESTRICT` | 审核通过后创建的正式菜品 ID。 |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 提交时间。 |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 记录最后更新时间。 |
| 组合约束 | — | `CHECK ((status = 'approved') = (approved_dish_id IS NOT NULL))`；`CHECK (status = 'pending' OR (reviewed_by_user_id IS NOT NULL AND reviewed_at IS NOT NULL))`；`CHECK (status <> 'approved' OR price IS NOT NULL)` | 保证审核状态、审核信息、价格与正式菜品关联一致。 |

# content_moderation_logs（内容审核日志表）

| 字段 | PostgreSQL 类型 | 约束 / 默认值 | 简要说明 |
| --- | --- | --- | --- |
| `id` | `UUID` | `PRIMARY KEY` | 审核日志唯一标识，由后端生成。 |
| `review_submission_id` | `UUID` | 可空；`REFERENCES dish_review_submissions(id) ON DELETE RESTRICT` | 评论审核提交 ID；评论审核日志使用。 |
| `dish_submission_id` | `UUID` | 可空；`REFERENCES dish_submissions(id) ON DELETE RESTRICT` | 菜品审核提交 ID；菜品审核日志使用。 |
| `stall_submission_id` | `UUID` | 可空；`REFERENCES stall_submissions(id) ON DELETE RESTRICT` | 档口审核提交 ID；档口审核日志使用。 |
| `dish_image_id` | `UUID` | 可空；`REFERENCES dish_images(id) ON DELETE RESTRICT` | 独立图片审核目标；经项目负责人确认由 V4 新增。 |
| `moderator_user_id` | `UUID` | `NOT NULL REFERENCES users(id) ON DELETE RESTRICT` | 执行审核的用户 ID；权限由服务端校验。 |
| `action` | `VARCHAR(20)` | `NOT NULL CHECK (action IN ('approve', 'reject'))` | 审核动作；每次操作新增一条记录。 |
| `reason` | `TEXT` | 可空 | 审核说明或拒绝原因。 |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 审核操作时间。 |
| 组合约束 | — | `CHECK (num_nonnulls(review_submission_id, dish_submission_id, stall_submission_id, dish_image_id) = 1)` | V4 扩展为四类，每条日志必须且只能关联一个目标。 |

图片审核扩展保留 V1–V3 历史迁移，仅通过 V4 增量修改。新审核通过或驳回操作同时写入审核人、时间、原因及对应日志；旧图片审核信息允许为空，不虚构历史审核。菜品投稿审核时附带图片在同一事务通过，沿用该菜品投稿日志，不重复伪造独立图片审核事件。

# invitation_codes（邀请码表）

| 字段 | PostgreSQL 类型 | 约束 / 默认值 | 简要说明 |
| --- | --- | --- | --- |
| `id` | `UUID` | `PRIMARY KEY` | 邀请码唯一标识，由后端生成。 |
| `code_hash` | `TEXT` | `NOT NULL UNIQUE` | 邀请码的HMAC-SHA256摘要，不存明文。 |
| `label` | `VARCHAR(100)` | 可空 | 管理备注，如发放对象或用途。 |
| `max_uses` | `INTEGER` | 可空，`CHECK (max_uses IS NULL OR max_uses >= 1)` | 可复用次数上限，空表示不限次数。 |
| `used_count` | `INTEGER` | `NOT NULL DEFAULT 0 CHECK (used_count >= 0)` | 已使用次数。 |
| `expires_at` | `TIMESTAMPTZ` | 可空 | 到期时间，空表示长期有效。 |
| `status` | `VARCHAR(20)` | `NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'disabled'))` | 邀请码状态。 |
| `created_by_user_id` | `UUID` | 可空，`REFERENCES users(id) ON DELETE SET NULL` | 发放人，仅平台超级管理员可发放。 |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 记录创建时间。 |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 记录最后更新时间，由触发器维护。 |

注册可选填写邀请码，仅记录归因关系，不填不影响注册；发放、次数、有效期均为接口约定，数据库设计不规定必填性。V7 新增，已有迁移不变。

# ai_moderation_results（AI预检结论表）

| 字段 | PostgreSQL 类型 | 约束 / 默认值 | 简要说明 |
| --- | --- | --- | --- |
| `id` | `UUID` | `PRIMARY KEY` | 结论唯一标识，由后端生成。 |
| `submission_type` | `VARCHAR(20)` | `NOT NULL CHECK (submission_type IN ('review', 'photo', 'community-post', 'community-image'))` | 投稿类型：评论、图片、社区动态正文或社区配图（V13 放开社区动态，V14 补社区评论）。 |
| `submission_id` | `UUID` | `NOT NULL` | 对应投稿ID；组合唯一保证每条投稿最多一条结论。 |
| `risk_level` | `VARCHAR(20)` | `NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH'))` | 风险等级，只做分诊。 |
| `confidence` | `NUMERIC(4, 3)` | `NOT NULL CHECK (confidence >= 0 AND confidence <= 1)` | 置信度。 |
| `labels` | `TEXT[]` | `NOT NULL DEFAULT '{}'` | AI打标数组，原文透传。 |
| `reason` | `TEXT` | 可空 | 一句话中文说明。 |
| `model` | `VARCHAR(100)` | `NOT NULL` | 诊断模型标识。 |
| `checked_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 诊断时间。 |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | 记录创建时间。 |

结论行只读不改；同一投稿重复送检靠唯一约束去重。V9 新增，已有迁移不变。


# 2026-10-20 V15 实时评分修订（覆盖上文旧每日快照约定）

- `dish_reviews` 新增可空 `deleted_at TIMESTAMPTZ`；唯一约束改为 `(dish_id,user_id) WHERE deleted_at IS NULL`，按菜品和作者添加未删除记录的时间分页索引。身份和首次创建时间不可修改，已删除记录不可恢复或编辑。
- 新增 `dish_stats`：`dish_id UUID PRIMARY KEY REFERENCES dishes ON DELETE CASCADE`、`review_count BIGINT NOT NULL DEFAULT 0`、`rating_sum BIGINT NOT NULL DEFAULT 0`、`counter_version BIGINT NOT NULL DEFAULT 0`、`updated_at TIMESTAMPTZ NOT NULL DEFAULT now()`。各数值非负，评分总和处于评论数至评论数×5之间。
- 统计只保存未删除正式点评。新建+1/+评分；重评只更新新旧评分差；删除-1/-旧评分；待审核、驳回不计入。统计由写事务触发器维护。
- `dish_rating_summary` 替换为普通视图，均分为 `ROUND(rating_sum::NUMERIC/review_count,2)`，零评论时NULL。每日及启动刷新均已取消。
- 现有用户、菜品、审核提交外键及历史保留规则继续适用；完整实现见 V15 迁移。
