INSERT INTO energosbyt.fact_gauge_bindings
    (gauge_id, realty_id, created_at, sign, version)
SELECT gauge_id, realty_id, created_at, sign, version
FROM input('gauge_id UInt64,
            realty_id UInt64,
            created_at DateTime,
            sign Int8,
            version UInt64')
FORMAT CSVWithNames
