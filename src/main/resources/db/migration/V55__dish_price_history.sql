ALTER TABLE foodtime.dishes ADD COLUMN price_unit TEXT NOT NULL DEFAULT 'unspecified'
    CHECK (price_unit IN ('unspecified','serving','piece','bowl','jin','100g','kg'));

ALTER TABLE foodtime.catalog_corrections
    ADD COLUMN original_price_unit TEXT NOT NULL DEFAULT 'unspecified',
    ADD COLUMN proposed_price_unit TEXT;
ALTER TABLE foodtime.catalog_corrections DROP CONSTRAINT ck_catalog_corrections_fields;
ALTER TABLE foodtime.catalog_corrections ADD CONSTRAINT ck_catalog_corrections_fields
    CHECK (cardinality(changed_fields) BETWEEN 1 AND 4
        AND changed_fields <@ ARRAY['name','description','price','priceUnit']::text[]);
ALTER TABLE foodtime.catalog_corrections ADD CONSTRAINT ck_catalog_corrections_price_unit
    CHECK (proposed_price_unit IS NULL OR proposed_price_unit IN ('unspecified','serving','piece','bowl','jin','100g','kg'));

CREATE TABLE foodtime.dish_price_history (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dish_id UUID NOT NULL REFERENCES foodtime.dishes(id) ON DELETE CASCADE,
    price NUMERIC(10,2) CHECK (price IS NULL OR price >= 0),
    price_unit TEXT NOT NULL CHECK (price_unit IN ('unspecified','serving','piece','bowl','jin','100g','kg')),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    origin TEXT NOT NULL CHECK (origin IN ('baseline','created','changed')),
    hidden_at TIMESTAMPTZ,
    revision BIGINT NOT NULL DEFAULT 1 CHECK (revision > 0)
);
CREATE INDEX ix_dish_price_history_visible ON foodtime.dish_price_history(dish_id,id)
    WHERE hidden_at IS NULL;

-- Existing prices are observations at installation, never invented historical selling dates.
INSERT INTO foodtime.dish_price_history(dish_id,price,price_unit,origin)
    SELECT id,price,price_unit,'baseline' FROM foodtime.dishes ORDER BY id;

-- Capture every writer, including approved corrections and newly approved submissions.
-- The history-management transaction suppresses capture only while correcting current state.
CREATE FUNCTION foodtime.capture_dish_price() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF current_setting('foodtime.correcting_price_history', true) = 'on' THEN RETURN NEW; END IF;
    IF TG_OP = 'INSERT' THEN
        INSERT INTO foodtime.dish_price_history(dish_id,price,price_unit,origin)
            VALUES (NEW.id,NEW.price,NEW.price_unit,'created');
    ELSIF NEW.price IS DISTINCT FROM OLD.price OR NEW.price_unit IS DISTINCT FROM OLD.price_unit THEN
        INSERT INTO foodtime.dish_price_history(dish_id,price,price_unit,origin,recorded_at)
            VALUES (NEW.id,NEW.price,NEW.price_unit,'changed',GREATEST(clock_timestamp(),
                COALESCE((SELECT max(recorded_at) FROM foodtime.dish_price_history WHERE dish_id=NEW.id),'-infinity')));
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER capture_dish_price AFTER INSERT OR UPDATE OF price,price_unit ON foodtime.dishes
    FOR EACH ROW EXECUTE FUNCTION foodtime.capture_dish_price();

-- Audit survives physical catalog removal; it is never part of the user-facing timeline.
CREATE TABLE foodtime.dish_price_history_audit (
    id UUID PRIMARY KEY,
    dish_id UUID NOT NULL,
    history_id BIGINT NOT NULL,
    actor_id UUID REFERENCES foodtime.users(id) ON DELETE SET NULL,
    action TEXT NOT NULL CHECK (action IN ('corrected','hidden')),
    before_values JSONB NOT NULL,
    after_values JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);
