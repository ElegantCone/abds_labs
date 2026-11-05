package org.itmo.backend.utils;

import java.util.Map;

import static org.itmo.backend.utils.ExceptionUtils.invalid;
import static org.itmo.backend.utils.ExceptionUtils.invalidParameter;

public class ValidationUtils {

    public static String validatePhone(String phone) {
        if (phone != null && !phone.matches("\\+[1-9][0-9]{6,14}")) throw invalid("phone");
        return phone;
    }

    public static void validateDate(Object value, String fieldName) {
        if (value == null || !value.toString().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw invalid(fieldName);
        }
    }

    public static void validateUnsigned(String value, String fieldName) {
        if (value == null || !value.matches("[0-9]+")) throw invalidParameter(fieldName);
    }

    public static boolean atLeastOnePresented(Map<String, ?> objects, String... names) {
        if (objects == null || objects.isEmpty()) throw invalid("objects");
        for (String name : names) {
            if (objects.containsKey(name)) return true;
        }
        return false;
    }
}
