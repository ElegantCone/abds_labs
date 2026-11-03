INSERT INTO energosbyt.dim_users (id, fio, phone, phone_prev, created_at, updated_at)
SELECT id, fio, phone, phone_prev, created_at, updated_at
FROM input('id         UInt64,
    fio        String,
    phone      String,
    phone_prev String,
    created_at DateTime,
    updated_at DateTime')
FORMAT CSVWithNames