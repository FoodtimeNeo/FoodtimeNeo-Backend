-- 按楼层拆分属于部署备注，不应对用户展示；清空后前端统一显示默认文案。
-- 仅清空这批新厅的内部说明，不动其他字段与其他食堂。
UPDATE foodtime.dining_halls
SET description = NULL
WHERE id IN (
    'f158d27a-329c-57ec-978a-22ed9e6a447b',
    'bb05ca8f-2544-5e2a-a7e1-151a8b3e0e8f',
    'd4fb48b3-a92d-5e93-a4f3-4ab3f210118d',
    'af829558-1410-503a-a426-e1d7ba2a399d',
    '9bc79377-bb58-54ff-8b51-4cfb67411d37'
);
