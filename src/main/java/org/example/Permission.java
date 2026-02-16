package org.example;

public record Permission(
        String name,
        String resource,
        String description
) {

    public Permission {
        if (name == null || resource == null || description == null || name.isBlank() || resource.isBlank() || description.isBlank()) {
            throw new IllegalArgumentException("Все поля должны содержать значения");
        }

        if (name.contains(" ")) {
            throw new IllegalArgumentException("name не должно содержать пробелов");
        }

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
