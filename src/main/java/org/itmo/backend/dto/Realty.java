package org.itmo.backend.dto;

import java.sql.ResultSet;
import java.sql.SQLException;

public record Realty(long id, String address, long square, String type, String number, int rooms, int floor, int porch) {

    public static Realty fromResultSet(ResultSet resultSet) throws SQLException {
        return new Realty(
                resultSet.getLong("id"),
                resultSet.getString("address"),
                resultSet.getLong("square"),
                resultSet.getString("type"),
                resultSet.getString("number"),
                resultSet.getInt("rooms"),
                resultSet.getInt("floor"),
                resultSet.getInt("porch")
        );
    }
}
