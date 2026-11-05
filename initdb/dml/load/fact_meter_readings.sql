INSERT INTO fact_meter_readings
    (realty_id, user_id, tariff_id, gauge_id, reading_date, day_kw, night_kw, created_at)
SELECT realty_id, user_id, tariff_id, gauge_id, reading_date, day_kw, night_kw, created_at
FROM input('realty_id UInt64,
            user_id UInt64,
            tariff_id UInt64,
            gauge_id UInt64,
            reading_date Date,
            day_kw UInt32,
            night_kw UInt32,
            created_at DateTime')
FORMAT CSVWithNames