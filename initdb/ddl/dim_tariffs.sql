CREATE TABLE IF NOT EXISTS dim_tariffs
(
    id               UInt64,
    name             String,
    price_day_kwph   UInt32,
    price_night_kwph UInt32,
    valid_from       DateTime,
    valid_to         DateTime,
    created_at       DateTime,
    updated_at       DateTime
)
ENGINE = ReplacingMergeTree(updated_at)
ORDER BY (id, valid_from);
