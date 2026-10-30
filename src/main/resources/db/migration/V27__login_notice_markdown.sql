-- Preserve IDs, timestamps and historical read rows. Content is a single Markdown document.
ALTER TABLE foodtime.notices ADD COLUMN content TEXT;
UPDATE foodtime.notices
SET content = CASE WHEN btrim(title) = '' THEN body ELSE '# ' || title || E'\n\n' || body END;
ALTER TABLE foodtime.notices ALTER COLUMN content SET NOT NULL;
ALTER TABLE foodtime.notices DROP COLUMN title;
ALTER TABLE foodtime.notices DROP COLUMN body;
ALTER TABLE foodtime.notices RENAME COLUMN id TO notice_id;
ALTER TABLE foodtime.notices RENAME COLUMN published_at TO created_time;
-- seq is retained only for the deprecated feed contract used by older clients.
ALTER TABLE foodtime.notice_reads
    ADD COLUMN dont_show_again BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE foodtime.config (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    notice_id UUID NULL REFERENCES foodtime.notices(notice_id) ON DELETE SET NULL
);
INSERT INTO foodtime.config(id, notice_id) VALUES (1, NULL);
COMMENT ON TABLE foodtime.config IS '单行全局配置，固定 id=1；notice_id 为空时没有登录公告';
COMMENT ON COLUMN foodtime.notices.content IS '完整 Markdown 公告，不拆分标题和正文';
COMMENT ON COLUMN foodtime.notice_reads.dont_show_again IS '用户确认后不再弹出该公告；与普通已读状态独立';
