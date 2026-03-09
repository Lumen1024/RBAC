package org.example.data;

import org.example.utils.ValidationUtils;

public record Permission(
        String name,
        String resource,
        String description
) {

    public Permission {
        ValidationUtils.requireNonEmpty(name, "name");
        ValidationUtils.requireNonEmpty(resource, "resource");
        ValidationUtils.requireNonEmpty(description, "description");
        if (name.contains(" "))
            throw new IllegalArgumentException("name не должно содержать пробелов");

        name = name.toUpperCase();
        resource = resource.toLowerCase();
    }

    public String format() {
        return "%s on %s: %s".formatted(name, resource, description);
    }

    public boolean matches(String namePattern, String resourcePattern) {
        return name.contains(namePattern.toUpperCase()) && resource.contains(resourcePattern.toLowerCase());
    }
}
