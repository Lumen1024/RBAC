package org.example;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Role {
    private final String id;
    private final String name;
    private final String description;
    private final Set<Permission> permissions;

    public Role(String name, String description) {
        if (name == null || name.isBlank() || description == null || description.isBlank()) {
            throw new IllegalArgumentException("Все поля должны содержать значения");
        }

        this.id = "role_" + UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.permissions = new HashSet<>();
    }

    // region permissions

    public void addPermission(Permission permission) {
        permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        permissions.remove(permission);
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public boolean hasPermission(String permissionName, String resource) {
        return permissions.stream().anyMatch(p -> p.name().equalsIgnoreCase(permissionName) && p.resource().equalsIgnoreCase(resource));
    }

    // endregion

    // region getters

    public Set<Permission> getPermissions() {
        return Set.copyOf(permissions);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    // endregion

    public String format() {
        String perms = permissions.stream().map(p -> "  - " + p.format()).collect(Collectors.joining("\n"));
        return "Role: %s [ID: %s]\nDescription: %s\nPermissions (%d):\n%s".formatted(name, id, description, permissions.size(), perms);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        return id.equals(role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Role{id='%s', name='%s', permissions=%d}".formatted(id, name, permissions.size());
    }
}
