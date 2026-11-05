CREATE TABLE IF NOT EXISTS dim_users
(
    id         UInt64,
    fio        String,
    phone      String,
    phone_prev String,
    created_at DateTime,
    updated_at DateTime
)
ENGINE = ReplacingMergeTree(updated_at)
ORDER BY id;
