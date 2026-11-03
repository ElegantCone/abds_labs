INSERT INTO energosbyt.dim_realty
    (id, address, square, `type`, number, rooms, floor, porch, created_at)
SELECT id,address,square,`type`,number,rooms,floor,porch,created_at
FROM input('id UInt64,
            address String,
            square UInt32,
            `type` String,
            number String,
            rooms UInt8,
            floor UInt8,
            porch UInt8,
            created_at DateTime')
FORMAT CSVWithNames