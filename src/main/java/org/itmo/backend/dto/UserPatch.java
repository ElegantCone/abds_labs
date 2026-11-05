package org.itmo.backend.dto;

import org.itmo.backend.utils.ValidationUtils;

import java.util.Map;

import static org.itmo.backend.utils.ExceptionUtils.invalid;
import static org.itmo.backend.utils.ExceptionUtils.optionalStringField;

public record UserPatch(String fio, String phone) {
    public static UserPatch from(Map<String, String> fields) {
        if (!ValidationUtils.atLeastOnePresented(fields, "fio", "phone")) throw invalid("fio");
        String fio = optionalStringField(fields, "fio");
        String phone = optionalStringField(fields, "phone");
        return new UserPatch(fio, ValidationUtils.validatePhone(phone));
    }
}
