ALTER TABLE advertising_campaigns
    ALTER COLUMN start_date TYPE DATE USING start_date::date,
    ALTER COLUMN end_date TYPE DATE USING end_date::date;
