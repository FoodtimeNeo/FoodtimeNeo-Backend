-- 档口图集：首图同时为 stalls.cover_image_url，其余多图按 sort_order 排序展示。
-- Use schema-qualified names so this migration is independent of search_path.

CREATE TABLE foodtime.stall_images (
    id UUID PRIMARY KEY,
    stall_id UUID NOT NULL REFERENCES foodtime.stalls(id) ON DELETE RESTRICT,
    image_url TEXT NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_stall_images_stall_id ON foodtime.stall_images (stall_id);
COMMENT ON TABLE foodtime.stall_images IS '档口图集：首图同时为 stalls.cover_image_url';
COMMENT ON COLUMN foodtime.stall_images.stall_id IS '所属档口；删档口需先清图集';
COMMENT ON COLUMN foodtime.stall_images.image_url IS '媒体接口地址，如 /api/v1/media/<uuid>；文件见 seed-images/v25/';
COMMENT ON COLUMN foodtime.stall_images.sort_order IS '档口内展示顺序，0 为首图，与 stalls.cover_image_url 一致';
