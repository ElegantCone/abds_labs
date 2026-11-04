CREATE TABLE IF NOT EXISTS dim_realty
(
    id         UInt64,
    address    String,
    square     UInt32,
    `type`     String,
    number     String,
    rooms      UInt8,
    floor      UInt8,
    porch      UInt8,
    created_at DateTime
)
ENGINE = MergeTree()
ORDER BY (address, id)
SETTINGS enable_block_number_column = 1, enable_block_offset_column = 1;
