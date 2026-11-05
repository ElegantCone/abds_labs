package org.itmo.backend.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.itmo.backend.utils.ExceptionUtils;

import static org.itmo.backend.utils.ExceptionUtils.invalid;
import static org.itmo.backend.utils.ExceptionUtils.parseUnsigned;
import static org.itmo.backend.utils.ExceptionUtils.parseUInt;

public record Reading(@JsonProperty("user_id") Long userId,
                      @JsonProperty("gauge_id") Long gaugeId,
                      @JsonProperty("tariff_id") Long tariffId,
                      @JsonProperty("reading_date") LocalDate readingDate,
                      @JsonProperty("day_kw") Long dayKw,
                      @JsonProperty("night_kw") Long nightKw) {

    public static Reading fromMap(Map<String, Object> fields) {
        return new Reading(
                parseUnsigned(fields, "user_id"),
                parseUnsigned(fields, "gauge_id"),
                parseUnsigned(fields, "tariff_id"),
                date(fields.get("reading_date")),
                parseUInt(fields, "day_kw"),
                parseUInt(fields, "night_kw")
        );
    }

    public static Reading fromResultSet(ResultSet rs) throws SQLException {
        return new Reading(
                rs.getLong("user_id"),
                rs.getLong("gauge_id"),
                rs.getLong("tariff_id"),
                rs.getDate("reading_date").toLocalDate(),
                rs.getLong("day_kw"),
                rs.getLong("night_kw")
        );
    }

    private static LocalDate date(Object value) {
        LocalDate date = ExceptionUtils.date(value, "reading_date");
        if (date.toEpochDay() < 0 || date.toEpochDay() > LocalDate.now().toEpochDay()) {
            throw invalid("reading_date");
        }
        return date;
    }
}
