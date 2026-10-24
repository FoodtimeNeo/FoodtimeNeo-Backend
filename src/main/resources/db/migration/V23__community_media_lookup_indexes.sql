-- 反查发布可见性与暂存清理按图片引用查询，不能只依赖以 post_id 开头的联合索引。
CREATE INDEX ix_community_images_reference ON foodtime.community_post_images(image_url);
CREATE INDEX ix_community_author_avatar ON foodtime.users(avatar_url)
    WHERE avatar_url IS NOT NULL AND status='active';
