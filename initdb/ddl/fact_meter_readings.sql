CREATE TABLE IF NOT EXISTS fact_meter_readings
(
    realty_id    UInt64,
    user_id      UInt64,
    tariff_id    UInt64,
    gauge_id     UInt64,
    reading_date Date,
    day_kw       UInt32,
    night_kw     UInt32,
    created_at   DateTime
)
ENGINE = ReplacingMergeTree(created_at)
PARTITION BY toYYYYMM(reading_date)
ORDER BY (realty_id, gauge_id, reading_date);
