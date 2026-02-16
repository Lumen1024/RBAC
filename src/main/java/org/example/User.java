package org.example;

import java.util.regex.Pattern;

public record User(String username, String fullName, String email) {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@]+@[^@]+\\.[^@]+$");


    public static User create(String username, String fullName, String email) {
        if (username == null || fullName == null || email == null || username.isBlank() || fullName.isBlank() || email.isBlank()) {
            throw new IllegalArgumentException("Все поля должны содержать значения");
        }

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("username должен содержать только латинские буквы, цифры и подчёркивание (3–20 символов)");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("email должен соответствовать формату");
        }
        return new User(username, fullName, email);
    }

    public String format() {
        return "%s (%s) <%s>".formatted(username, fullName, email);
    }
}
