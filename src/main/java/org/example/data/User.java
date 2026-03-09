package org.example.data;

import org.example.utils.ValidationUtils;

import java.util.regex.Pattern;

public record User(
        String username,
        String fullName,
        String email
) {

    public static User create(String username, String fullName, String email) {
        ValidationUtils.requireNonEmpty(username, "username");
        ValidationUtils.requireNonEmpty(fullName, "fullName");
        ValidationUtils.requireNonEmpty(email, "email");

        if(ValidationUtils.isValidUsername(username))
            throw new IllegalArgumentException("username должен содержать только латинские буквы, цифры и подчёркивание (3–20 символов)");
        if (ValidationUtils.isValidEmail(email))
            throw new IllegalArgumentException("email должен соответствовать формату");

        return new User(username, fullName, email);
    }

    public String format() {
        return "%s (%s) <%s>".formatted(username, fullName, email);
    }
}
