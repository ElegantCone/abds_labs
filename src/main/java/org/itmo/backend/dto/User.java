package org.itmo.backend.dto;

import java.sql.ResultSet;
import java.sql.SQLException;

public record User(long id, String fio, String phone, String phone_prev) {

    public static User fromResultSet(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("fio"),
                rs.getString("phone"),
                rs.getString("phone_prev")
        );
    }
}
