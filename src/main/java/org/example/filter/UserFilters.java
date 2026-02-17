package org.example.filter;

import java.util.Locale;

public class UserFilters {
    static UserFilter byUsername(String username) {
        return user -> user.username().equals(username);
    }

    static UserFilter byUsernameContains(String substring) {
        return user -> user.username().toLowerCase().contains(substring.toLowerCase(Locale.ROOT));
    }

    static UserFilter byEmail(String email) {
        return user -> user.email().equals(email);
    }

    static UserFilter byEmailDomain(String domain) {
        return user -> user.email().matches(".*@%s".formatted(domain));
    }

    static UserFilter byFullNameContains(String substring) {
        return user -> user.fullName().toLowerCase(Locale.ROOT).contains(substring.toLowerCase());
    }

}
