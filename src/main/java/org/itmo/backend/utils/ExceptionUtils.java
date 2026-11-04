package org.itmo.backend.utils;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ExceptionUtils {
    private ExceptionUtils() {
    }

    public static InvalidRequestException invalid(String name) {
        return new InvalidRequestException("invalid \"" + name + "\" field");
    }

    public static InvalidRequestException invalidParameter(String name) {
        return new InvalidRequestException("invalid \"" + name + "\" parameter");
    }

    public static long parseUnsigned(Map<String, Object> fields, String name) {
        Object obj = fields.get(name);
        if (!(obj instanceof Number)) {
            throw invalid(name);
        }
        long value;
        try {
            value = obj instanceof BigInteger number ? number.longValueExact() : ((Number) obj).longValue();
        } catch (ArithmeticException error) {
            throw invalid(name);
        }
        if (value < 0) throw invalid(name);
        return value;
    }

    public static long parseUInt(Map<String, Object> fields, String name) {
        long value = parseUnsigned(fields, name);
        //only < unsigned int
        if (value > 0xffff_ffffL) throw invalid(name);
        return value;
    }

    public static String optionalStringField(Map<String, String> fields, String name) {
        var value = fields.get(name);
        if (fields.containsKey(name) && (value == null || value.isBlank())) throw invalid(name);
        return value;
    }

    public static LocalDate date(Object value, String name) {
        ValidationUtils.validateDate(value, name);
        try {
            return LocalDate.parse(value.toString());
        } catch (DateTimeParseException cause) {
            throw invalid(name);
        }
    }

    public static long unsignedParameter(Map<String, String> parameters, String name, long defaultValue) {
        if (!parameters.containsKey(name)) return defaultValue;
        var value = parameters.get(name);
        ValidationUtils.validateUnsigned(value, name);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException error) {
            throw invalidParameter(name);
        }
    }

    public static ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    public static ResponseEntity<?> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Not found"));
    }

    public static ResponseEntity<?> found(Optional<?> value) {
        if (value.isEmpty()) return notFound();
        return ResponseEntity.ok(value.get());
    }

    public static final class InvalidRequestException extends IllegalArgumentException {
        private InvalidRequestException(String message) {
            super(message);
        }
    }
}
