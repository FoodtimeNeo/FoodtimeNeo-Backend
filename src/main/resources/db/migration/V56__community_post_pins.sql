CREATE TABLE foodtime.community_pin_state (
    singleton boolean PRIMARY KEY DEFAULT true CHECK (singleton),
    revision bigint NOT NULL DEFAULT 0
);
INSERT INTO foodtime.community_pin_state(singleton) VALUES (true);

CREATE TABLE foodtime.community_post_pins (
    post_id uuid PRIMARY KEY REFERENCES foodtime.community_posts(id) ON DELETE CASCADE,
    position smallint NOT NULL CHECK (position BETWEEN 1 AND 3),
    pinned_by uuid REFERENCES foodtime.users(id) ON DELETE SET NULL,
    pinned_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    UNIQUE(position) DEFERRABLE INITIALLY DEFERRED
);

-- Visibility changes invalidate discovery pagination, without discarding a pending edit's slot.
CREATE FUNCTION foodtime.community_pin_visibility_changed() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' THEN
        IF EXISTS(SELECT 1 FROM foodtime.community_post_pins WHERE post_id=OLD.id) THEN
            PERFORM 1 FROM foodtime.community_pin_state WHERE singleton FOR UPDATE;
            DELETE FROM foodtime.community_post_pins WHERE post_id=OLD.id;
            WITH ordered AS (SELECT post_id,row_number() OVER (ORDER BY position) AS slot FROM foodtime.community_post_pins)
            UPDATE foodtime.community_post_pins pin SET position=ordered.slot FROM ordered WHERE pin.post_id=ordered.post_id;
            UPDATE foodtime.community_pin_state SET revision=revision+1 WHERE singleton;
        END IF;
        RETURN OLD;
    END IF;
    IF NEW.status IS DISTINCT FROM OLD.status OR NEW.deleted_at IS DISTINCT FROM OLD.deleted_at THEN
        IF EXISTS(SELECT 1 FROM foodtime.community_post_pins WHERE post_id=NEW.id) THEN
            PERFORM 1 FROM foodtime.community_pin_state WHERE singleton FOR UPDATE;
            IF NEW.deleted_at IS NOT NULL THEN
                DELETE FROM foodtime.community_post_pins WHERE post_id=NEW.id;
                WITH ordered AS (SELECT post_id,row_number() OVER (ORDER BY position) AS slot FROM foodtime.community_post_pins)
                UPDATE foodtime.community_post_pins pin SET position=ordered.slot FROM ordered WHERE pin.post_id=ordered.post_id;
            END IF;
            UPDATE foodtime.community_pin_state SET revision=revision+1 WHERE singleton;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER community_pin_visibility_changed
AFTER UPDATE OF status,deleted_at ON foodtime.community_posts
FOR EACH ROW EXECUTE FUNCTION foodtime.community_pin_visibility_changed();
CREATE TRIGGER community_pin_post_deleted BEFORE DELETE ON foodtime.community_posts
FOR EACH ROW EXECUTE FUNCTION foodtime.community_pin_visibility_changed();
