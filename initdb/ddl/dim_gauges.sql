CREATE TABLE IF NOT EXISTS dim_gauges
(
    id         UInt64,
    name       String,
    country    String,
    created_at DateTime
)
ENGINE = MergeTree
ORDER BY (name, id);
