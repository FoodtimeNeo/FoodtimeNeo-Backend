ALTER TABLE foodtime.community_posts
    ADD COLUMN dish_id UUID REFERENCES foodtime.dishes(id) ON DELETE RESTRICT;
CREATE INDEX ix_community_posts_dish ON foodtime.community_posts(dish_id) WHERE dish_id IS NOT NULL;

CREATE TABLE foodtime.dish_favorites (
    user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    dish_id UUID NOT NULL REFERENCES foodtime.dishes(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, dish_id)
);
CREATE INDEX ix_dish_favorites_user_recent ON foodtime.dish_favorites(user_id, created_at DESC, dish_id DESC);
CREATE INDEX ix_dish_favorites_dish ON foodtime.dish_favorites(dish_id);
