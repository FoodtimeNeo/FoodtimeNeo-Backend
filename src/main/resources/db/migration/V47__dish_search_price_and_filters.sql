-- 价格未知是合法业务状态；0 仍是有效价格。
ALTER TABLE foodtime.dishes ALTER COLUMN price DROP NOT NULL;
COMMENT ON COLUMN foodtime.dishes.price IS '菜品价格；NULL 表示价格未知，0 是有效价格';

-- 筛选与稳定排序所需的读路径索引。
CREATE INDEX IF NOT EXISTS ix_dining_halls_status ON foodtime.dining_halls (status);
CREATE INDEX IF NOT EXISTS ix_stalls_dining_hall_id ON foodtime.stalls (dining_hall_id);
CREATE INDEX IF NOT EXISTS ix_stalls_status ON foodtime.stalls (status);
CREATE INDEX IF NOT EXISTS ix_dishes_status ON foodtime.dishes (status);
CREATE INDEX IF NOT EXISTS ix_dishes_price ON foodtime.dishes (price) WHERE status = 'active';
