CREATE TABLE IF NOT EXISTS fact_gauge_bindings
(
    gauge_id   UInt64,
    realty_id  UInt64,
    created_at DateTime,
    sign       Int8,
    version    UInt64
)
ENGINE = VersionedCollapsingMergeTree(sign, version)
ORDER BY (gauge_id, realty_id);
