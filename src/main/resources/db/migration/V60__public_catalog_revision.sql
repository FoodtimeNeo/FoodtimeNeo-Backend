-- Public projections only. No user/account/community/private data are replicated.
CREATE TABLE foodtime.public_catalog_revision (
    id smallint PRIMARY KEY CHECK (id = 1),
    revision bigint NOT NULL DEFAULT 1
);
INSERT INTO foodtime.public_catalog_revision(id) VALUES (1);

CREATE FUNCTION foodtime.bump_public_catalog_revision() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    UPDATE foodtime.public_catalog_revision SET revision = revision + 1 WHERE id = 1;
    RETURN NULL;
END;
$$;

-- Transactional invalidation also covers admin edits, approvals, corrections and SQL maintenance.
DO $$
DECLARE relation_name text;
BEGIN
    FOREACH relation_name IN ARRAY ARRAY['dining_halls','stalls','dishes','stall_images','dish_images',
                                         'dish_stats','venue_rating_stats','venue_review_targets']
    LOOP
        EXECUTE format('CREATE TRIGGER public_catalog_changed AFTER INSERT OR UPDATE OR DELETE OR TRUNCATE ON foodtime.%I FOR EACH STATEMENT EXECUTE FUNCTION foodtime.bump_public_catalog_revision()', relation_name);
    END LOOP;
END;
$$;
