-- V28 给学活一楼/二楼写入的封面 UUID 有笔误（bfb6d4cc-85e6-...），
-- 旧学活食堂的真实封面是 bfb6d4cc-05ec-5868-a697-12d2e8334780，
-- 对应文件 seed-images/v6/bfb6d4cc-05ec-5868-a697-12d2e8334780.jpg。
-- 这里把两个新厅指回旧封面；V28 已应用，不能修改其内容。
UPDATE foodtime.dining_halls
SET cover_image_url = '/api/v1/media/bfb6d4cc-05ec-5868-a697-12d2e8334780'
WHERE id IN (
    'af829558-1410-503a-a426-e1d7ba2a399d',
    '9bc79377-bb58-54ff-8b51-4cfb67411d37'
);
