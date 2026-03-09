package org.example.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ValidationUtils {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        return username.matches("[a-zA-Z0-9_]{3,20}");
    }

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");
    }

    public static boolean isValidDate(String date) {
        if (date == null) return false;
        try {
            LocalDate.parse(date, DATE_FORMAT);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static String normalizeString(String input) {
        if (input == null) return null;
        return input.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Поле '" + fieldName + "' не может быть пустым");
        }
    }
}