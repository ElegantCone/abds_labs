package org.itmo.backend.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record Invoice(long tariff_id,
                      long gauge_id,
                      long day_kw,
                      long night_kw,
                      long price,
                      LocalDate deadline,
                      @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime created_at) {
    public static Invoice fromResultSet(ResultSet rs) throws SQLException {
        return new Invoice(
                rs.getLong("tariff_id"),
                rs.getLong("gauge_id"),
                rs.getLong("day_kw"),
                rs.getLong("night_kw"),
                rs.getLong("price"),
                rs.getDate("deadline").toLocalDate(),
                rs.getObject("created_at", LocalDateTime.class)
        );
    }
}
