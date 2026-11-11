DO $$
DECLARE
    status_check_name TEXT;
BEGIN
    SELECT conname INTO status_check_name
    FROM pg_constraint
    WHERE conrelid = 'delivery_area'::regclass
      AND contype = 'c'
      AND pg_get_constraintdef(oid) LIKE '%status%';

    IF status_check_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE delivery_area DROP CONSTRAINT %I', status_check_name);
    END IF;
END $$;
UPDATE delivery_area
SET status = CASE status
    WHEN 'DELIVERING' THEN 'SUPPORTED'
    ELSE 'NOT_SUPPORTED'
END;
ALTER TABLE delivery_area ALTER COLUMN status SET DEFAULT 'NOT_SUPPORTED';
ALTER TABLE delivery_area
    ADD CONSTRAINT delivery_area_status_supported_check
    CHECK (status IN ('SUPPORTED', 'NOT_SUPPORTED'));

ALTER TABLE delivery_area ALTER COLUMN place_id DROP NOT NULL;
ALTER TABLE delivery_area ALTER COLUMN formatted_address DROP NOT NULL;
ALTER TABLE delivery_area ALTER COLUMN locality DROP NOT NULL;
ALTER TABLE delivery_area ALTER COLUMN city DROP NOT NULL;
ALTER TABLE delivery_area ALTER COLUMN latitude DROP NOT NULL;
ALTER TABLE delivery_area ALTER COLUMN longitude DROP NOT NULL;
ALTER TABLE delivery_area ADD COLUMN area_type VARCHAR(16) NOT NULL DEFAULT 'PLACE';
ALTER TABLE delivery_area ADD COLUMN boundary JSONB;
ALTER TABLE delivery_area
    ADD CONSTRAINT delivery_area_type_check CHECK (
        (area_type = 'PLACE'
            AND place_id IS NOT NULL
            AND formatted_address IS NOT NULL
            AND locality IS NOT NULL
            AND city IS NOT NULL
            AND latitude IS NOT NULL
            AND longitude IS NOT NULL
            AND boundary IS NULL)
        OR
        (area_type = 'REGION'
            AND place_id IS NULL
            AND latitude IS NULL
            AND longitude IS NULL
            AND jsonb_typeof(boundary) = 'array')
    );
