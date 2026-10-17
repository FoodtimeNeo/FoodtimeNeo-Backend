-- Hot-path lookup indexes. Existing queries filter or join on these columns without
-- supporting indexes: stall listing by hall, public reviews by dish, image authorization
-- by reference, and orphan-cleanup reference checks. Index-only migration, no table changes.

CREATE INDEX ix_stalls_hall_id ON foodtime.stalls (dining_hall_id);
CREATE INDEX ix_dish_reviews_dish_id ON foodtime.dish_reviews (dish_id);
CREATE INDEX ix_dish_images_image_url ON foodtime.dish_images (image_url);
CREATE INDEX ix_stalls_cover_image_url ON foodtime.stalls (cover_image_url);
CREATE INDEX ix_dining_halls_cover_image_url ON foodtime.dining_halls (cover_image_url);
CREATE INDEX ix_users_avatar_url ON foodtime.users (avatar_url);
