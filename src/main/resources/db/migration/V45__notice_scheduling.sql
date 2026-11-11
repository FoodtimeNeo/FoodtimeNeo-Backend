ALTER TABLE foodtime.notices
    ADD COLUMN published_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN display_starts_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN display_ends_at TIMESTAMPTZ,
    ADD COLUMN deadline_date_only BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN is_update BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN published BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN publication_announced BOOLEAN NOT NULL DEFAULT false;

UPDATE foodtime.notices SET published_at=created_time,display_starts_at=created_time,
    publication_announced=true,
    enabled=coalesce(notice_id=(SELECT notice_id FROM foodtime.config WHERE id=1),false);

ALTER TABLE foodtime.notices
    ADD CONSTRAINT notices_schedule_order CHECK (display_starts_at>=published_at
        AND (display_ends_at IS NULL OR display_ends_at>display_starts_at)),
    ADD CONSTRAINT notices_enabled_published CHECK (NOT enabled OR published),
    ADD CONSTRAINT notices_day_deadline CHECK (NOT deadline_date_only OR display_ends_at IS NOT NULL);
CREATE INDEX ix_notices_scheduled ON foodtime.notices(published_at,seq)
    WHERE published AND NOT publication_announced;
CREATE INDEX ix_notices_popup ON foodtime.notices(is_update,published_at DESC,seq DESC)
    WHERE published AND enabled;
