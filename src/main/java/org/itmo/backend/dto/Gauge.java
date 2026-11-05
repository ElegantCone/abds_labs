package org.itmo.backend.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public record Gauge(long id, String name, String country, LocalDateTime created_at) {

    public static Gauge fromResultSet(ResultSet rs) throws SQLException {
        return new Gauge(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("country"),
                rs.getObject("created_at", LocalDateTime.class)
        );
    }
}
