package org.itmo.backend.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record Tariff (long id,
                      String name,
                      long price_day_kwph,
                      long price_night_kwph,
                      @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime valid_from,
                      @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime valid_to
) {
    public static Tariff fromResultSet(ResultSet resultSet) throws SQLException {
        return new Tariff(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getLong("price_day_kwph"),
                resultSet.getLong("price_night_kwph"),
                resultSet.getObject("valid_from", LocalDateTime.class),
                resultSet.getObject("valid_to", LocalDateTime.class)
        );
    }
}
