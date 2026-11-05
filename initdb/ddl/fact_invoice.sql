CREATE TABLE IF NOT EXISTS fact_invoice
(
    realty_id UInt64,
    user_id   UInt64,
    tariff_id UInt64,
    gauge_id  UInt64,
    day_kw     UInt32,
    night_kw   UInt32,
    price      UInt64,
    deadline   Date,
    created_at DateTime
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(created_at)
ORDER BY (realty_id, created_at, gauge_id);
