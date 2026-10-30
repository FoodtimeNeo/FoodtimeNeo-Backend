-- 解决 3 组价格不同的重名菜，按 2026-10-30 约定保留以下价格：
--   东区学生餐厅 / 重庆小面 / 酸菜肉丝面（汤拌）：13 元
--   学活二楼 / 西北味道 / 小炒肉面：16 元
--   学活二楼 / 西北味道 / 麻辣鸡丝面：13 元
-- V31 已删除 11 组完全一致的重复行；本迁移删除这 3 组中价格不符的行。
-- 在已手工处理的库上为 no-op。
WITH keep(hall, stall, dish, price) AS (
    VALUES
        ('东区学生餐厅', '重庆小面', '酸菜肉丝面（汤拌）', 13.00),
        ('学活二楼', '西北味道', '小炒肉面', 16.00),
        ('学活二楼', '西北味道', '麻辣鸡丝面', 13.00)
), targets AS (
    SELECT s.id AS stall_id, k.dish, k.price
    FROM keep k
    JOIN foodtime.dining_halls h ON h.name = k.hall
    JOIN foodtime.stalls s ON s.dining_hall_id = h.id AND s.name = k.stall
)
DELETE FROM foodtime.dishes d
USING targets t
WHERE d.stall_id = t.stall_id
  AND d.name = t.dish
  AND d.price <> t.price;
