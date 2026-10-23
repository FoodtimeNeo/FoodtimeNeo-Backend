# 社区数据库字段

本字段表来自独立验证库实际执行 V19–V23 后的结构。新增迁移不修改原有 V1–V18。

紧凑楼中楼：一级评论的 root_comment_id 为空，楼内所有回复都指向同一根评论；reply_to_comment_id 记录实际回复对象，被回复用户由服务端推导。帖子、评论和回复不可编辑。删除为软删除，保留审核证据；删除根评论时保留占位和合法回复。

AI 不确定或不可用转人工；仅 approved 且未删除的内容公开。标题 50 字、正文 2000 字、评论回复 500 字，原创配图 1–9 张。图片宽高由服务端核验。迁移前已有图片允许未知宽高，不伪造数据。

## community_posts（帖子）

| 字段 | 类型 | 可空 | 默认值 |
|---|---|---|---|
| id | uuid | 否 | — |
| user_id | uuid | 否 | — |
| content | text | 否 | — |
| repost_of_id | uuid | 是 | — |
| like_count | bigint | 否 | 0 |
| comment_count | bigint | 否 | 0 |
| repost_count | integer | 否 | 0 |
| status | character varying(20) | 否 | 'pending'::character varying |
| reviewed_by_user_id | uuid | 是 | — |
| reviewed_by_ai_model | text | 是 | — |
| reviewed_at | timestamp with time zone | 是 | — |
| review_reason | text | 是 | — |
| created_at | timestamp with time zone | 否 | now() |
| title | character varying(50) | 否 | '校园食记'::character varying |
| client_request_id | uuid | 否 | gen_random_uuid() |
| favorite_count | bigint | 否 | 0 |
| review_stage | character varying(20) | 否 | 'ai_pending'::character varying |
| manual_review_reason | character varying(32) | 是 | — |
| updated_at | timestamp with time zone | 否 | now() |
| deleted_at | timestamp with time zone | 是 | — |
| deleted_by_user_id | uuid | 是 | — |
| delete_reason | text | 是 | — |

约束：

- `CHECK ((favorite_count >= 0))`
- `CHECK (((char_length(btrim((title)::text)) >= 1) AND (char_length(btrim((title)::text)) <= 50)))`
- `CHECK (((char_length(content) >= 1) AND (char_length(content) <= 2000)))`
- `CHECK (((like_count >= 0) AND (comment_count >= 0) AND (repost_count >= 0)))`
- `CHECK (((deleted_at IS NULL) = (deleted_by_user_id IS NULL)))`
- `CHECK (((manual_review_reason IS NULL) OR ((manual_review_reason)::text = ANY ((ARRAY['uncertain'::character varying, 'ai_unavailable'::character varying, 'ai_disabled'::character varying])::text[]))))`
- `CHECK ((((review_stage)::text <> 'manual_pending'::text) OR (manual_review_reason IS NOT NULL)))`
- `CHECK ((((status)::text = 'pending'::text) OR ((reviewed_at IS NOT NULL) AND ((reviewed_by_user_id IS NOT NULL) OR (reviewed_by_ai_model IS NOT NULL)))))`
- `CHECK (((((status)::text = 'pending'::text) AND ((review_stage)::text = ANY ((ARRAY['ai_pending'::character varying, 'manual_pending'::character varying])::text[]))) OR (((status)::text = ANY ((ARRAY['approved'::character varying, 'rejected'::character varying])::text[])) AND ((review_stage)::text = 'completed'::text))))`
- `CHECK (((status)::text = ANY ((ARRAY['pending'::character varying, 'approved'::character varying, 'rejected'::character varying])::text[])))`
- `FOREIGN KEY (deleted_by_user_id) REFERENCES users(id) ON DELETE RESTRICT`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (repost_of_id) REFERENCES community_posts(id) ON DELETE RESTRICT`
- `FOREIGN KEY (reviewed_by_user_id) REFERENCES users(id) ON DELETE RESTRICT`
- `FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT`
- `UNIQUE (user_id, client_request_id)`

## community_post_images（帖子图片）

| 字段 | 类型 | 可空 | 默认值 |
|---|---|---|---|
| id | uuid | 否 | — |
| post_id | uuid | 否 | — |
| image_url | text | 否 | — |
| sort_order | integer | 否 | 0 |
| created_at | timestamp with time zone | 否 | now() |
| width | integer | 是 | — |
| height | integer | 是 | — |

约束：

- `CHECK (((width > 0) AND (height > 0) AND (((width)::bigint * height) <= 20000000)))`
- `CHECK (((width IS NOT NULL) AND (height IS NOT NULL))) NOT VALID`
- `CHECK (((sort_order >= 0) AND (sort_order <= 8)))`
- `CHECK ((sort_order >= 0))`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (post_id) REFERENCES community_posts(id) ON DELETE CASCADE`
- `UNIQUE (post_id, sort_order)`
- `UNIQUE (post_id, image_url)`

## community_comments（一级评论及回复）

| 字段 | 类型 | 可空 | 默认值 |
|---|---|---|---|
| id | uuid | 否 | — |
| post_id | uuid | 否 | — |
| user_id | uuid | 否 | — |
| content | text | 否 | — |
| like_count | bigint | 否 | 0 |
| status | character varying(20) | 否 | 'pending'::character varying |
| reviewed_by_user_id | uuid | 是 | — |
| reviewed_by_ai_model | text | 是 | — |
| reviewed_at | timestamp with time zone | 是 | — |
| review_reason | text | 是 | — |
| created_at | timestamp with time zone | 否 | now() |
| review_stage | character varying(20) | 否 | 'ai_pending'::character varying |
| manual_review_reason | character varying(32) | 是 | — |
| updated_at | timestamp with time zone | 否 | now() |
| deleted_at | timestamp with time zone | 是 | — |
| deleted_by_user_id | uuid | 是 | — |
| delete_reason | text | 是 | — |
| client_request_id | uuid | 否 | gen_random_uuid() |
| root_comment_id | uuid | 是 | — |
| reply_to_comment_id | uuid | 是 | — |
| reply_to_user_id | uuid | 是 | — |
| reply_count | bigint | 否 | 0 |

约束：

- `CHECK ((reply_count >= 0))`
- `CHECK ((((root_comment_id IS NULL) AND (reply_to_comment_id IS NULL) AND (reply_to_user_id IS NULL)) OR ((root_comment_id IS NOT NULL) AND (reply_to_comment_id IS NOT NULL) AND (reply_to_user_id IS NOT NULL) AND (root_comment_id <> id) AND (reply_to_comment_id <> id))))`
- `CHECK (((char_length(content) >= 1) AND (char_length(content) <= 500)))`
- `CHECK ((like_count >= 0))`
- `CHECK (((deleted_at IS NULL) = (deleted_by_user_id IS NULL)))`
- `CHECK (((manual_review_reason IS NULL) OR ((manual_review_reason)::text = ANY ((ARRAY['uncertain'::character varying, 'ai_unavailable'::character varying, 'ai_disabled'::character varying])::text[]))))`
- `CHECK ((((review_stage)::text <> 'manual_pending'::text) OR (manual_review_reason IS NOT NULL)))`
- `CHECK ((((status)::text = 'pending'::text) OR ((reviewed_at IS NOT NULL) AND ((reviewed_by_user_id IS NOT NULL) OR (reviewed_by_ai_model IS NOT NULL)))))`
- `CHECK (((((status)::text = 'pending'::text) AND ((review_stage)::text = ANY ((ARRAY['ai_pending'::character varying, 'manual_pending'::character varying])::text[]))) OR (((status)::text = ANY ((ARRAY['approved'::character varying, 'rejected'::character varying])::text[])) AND ((review_stage)::text = 'completed'::text))))`
- `CHECK (((status)::text = ANY ((ARRAY['pending'::character varying, 'approved'::character varying, 'rejected'::character varying])::text[])))`
- `FOREIGN KEY (deleted_by_user_id) REFERENCES users(id) ON DELETE RESTRICT`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (post_id) REFERENCES community_posts(id) ON DELETE CASCADE`
- `FOREIGN KEY (reply_to_comment_id) REFERENCES community_comments(id) ON DELETE RESTRICT`
- `FOREIGN KEY (reply_to_user_id) REFERENCES users(id) ON DELETE RESTRICT`
- `FOREIGN KEY (reviewed_by_user_id) REFERENCES users(id) ON DELETE RESTRICT`
- `FOREIGN KEY (root_comment_id) REFERENCES community_comments(id) ON DELETE RESTRICT`
- `FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT`
- `UNIQUE (user_id, client_request_id)`

## community_post_likes（帖子点赞）

| 字段 | 类型 | 可空 | 默认值 |
|---|---|---|---|
| user_id | uuid | 否 | — |
| post_id | uuid | 否 | — |
| created_at | timestamp with time zone | 否 | now() |

约束：

- `PRIMARY KEY (user_id, post_id)`
- `FOREIGN KEY (post_id) REFERENCES community_posts(id) ON DELETE CASCADE`
- `FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT`

## community_comment_likes（评论及回复点赞）

| 字段 | 类型 | 可空 | 默认值 |
|---|---|---|---|
| user_id | uuid | 否 | — |
| comment_id | uuid | 否 | — |
| created_at | timestamp with time zone | 否 | now() |

约束：

- `FOREIGN KEY (comment_id) REFERENCES community_comments(id) ON DELETE CASCADE`
- `PRIMARY KEY (user_id, comment_id)`
- `FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT`

## community_favorites（帖子收藏）

| 字段 | 类型 | 可空 | 默认值 |
|---|---|---|---|
| user_id | uuid | 否 | — |
| post_id | uuid | 否 | — |
| created_at | timestamp with time zone | 否 | now() |

约束：

- `FOREIGN KEY (post_id) REFERENCES community_posts(id) ON DELETE CASCADE`
- `FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT`
- `PRIMARY KEY (user_id, post_id)`

审核字段：status 表示 pending/approved/rejected；review_stage 表示 ai_pending/manual_pending/completed；manual_review_reason 表示 uncertain/ai_unavailable/ai_disabled。终审人或 AI 模型二选一，终审后不能改判。deleted_at、deleted_by_user_id、delete_reason 保存删除时间、操作者和原因。

计数由数据库触发器维护；点赞收藏联合唯一键防重，禁止更新明细行，使用增删表达意图。发布请求编号在每个作者范围内唯一，网络重试复用。新功能复用 users、ai_moderation_results、content_moderation_logs，不另建用户、AI结论或帖子版本表。

V23 为图片引用反查和启用用户头像增加索引。社区图片使用 no-store，每次请求检查当前发布及删除状态；目录图片沿用原有不可变缓存。
