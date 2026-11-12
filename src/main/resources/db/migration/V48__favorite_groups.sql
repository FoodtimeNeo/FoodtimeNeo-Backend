-- 菜品与帖子分组彼此独立；收藏事实仍在 dish_favorites / community_favorites。
CREATE TABLE foodtime.favorite_groups (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    kind VARCHAR(8) NOT NULL,
    name VARCHAR(40) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    revision BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_favorite_groups_kind CHECK (kind IN ('dish', 'post')),
    CONSTRAINT ck_favorite_groups_name CHECK (length(btrim(name)) BETWEEN 1 AND 20),
    CONSTRAINT uq_favorite_groups_id_owner UNIQUE (id, owner_id)
);
CREATE UNIQUE INDEX ux_favorite_groups_owner_kind_name
    ON foodtime.favorite_groups (owner_id, kind, name);
CREATE INDEX ix_favorite_groups_owner_kind_order
    ON foodtime.favorite_groups (owner_id, kind, sort_order, created_at, id);

-- 成员关系只允许指向“本人已收藏”的对象；取消收藏时关系随外键级联删除。
CREATE TABLE foodtime.dish_favorite_group_members (
    group_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    dish_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, dish_id),
    FOREIGN KEY (group_id, owner_id) REFERENCES foodtime.favorite_groups(id, owner_id) ON DELETE CASCADE,
    FOREIGN KEY (owner_id, dish_id) REFERENCES foodtime.dish_favorites(user_id, dish_id) ON DELETE CASCADE
);
CREATE INDEX ix_dish_fgm_owner_dish
    ON foodtime.dish_favorite_group_members (owner_id, dish_id);

CREATE TABLE foodtime.community_favorite_group_members (
    group_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    post_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, post_id),
    FOREIGN KEY (group_id, owner_id) REFERENCES foodtime.favorite_groups(id, owner_id) ON DELETE CASCADE,
    FOREIGN KEY (owner_id, post_id) REFERENCES foodtime.community_favorites(user_id, post_id) ON DELETE CASCADE
);
CREATE INDEX ix_community_fgm_owner_post
    ON foodtime.community_favorite_group_members (owner_id, post_id);
