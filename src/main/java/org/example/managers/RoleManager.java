package org.example.managers;

import org.example.data.Permission;
import org.example.data.Role;
import org.example.filter.RoleFilter;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RoleManager implements Repository<Role> {

    private final Map<String, Role> byId = new HashMap<>();
    private final Map<String, Role> byName = new HashMap<>();

    private Predicate<Role> assignmentCheck = null;

    public void setAssignmentCheck(Predicate<Role> check) {
        this.assignmentCheck = check;
    }

    @Override
    public void add(Role item) {
        Objects.requireNonNull(item, "Роль не может быть null");

        if (byName.containsKey(item.getName())) {
            throw new IllegalStateException(
                    "Роль с именем '%s' уже существует".formatted(item.getName()));
        }
        byId.put(item.getId(), item);
        byName.put(item.getName(), item);
    }

    @Override
    public boolean remove(Role item) {
        if (item == null) return false;
        if (!byId.containsKey(item.getId())) return false;

        if (assignmentCheck != null && assignmentCheck.test(item)) {
            throw new IllegalStateException(
                    "Роль '%s' назначена пользователям и не может быть удалена".formatted(item.getName()));
        }

        byId.remove(item.getId());
        byName.remove(item.getName());
        return true;
    }

    @Override
    public Optional<Role> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<Role> findAll() {
        return List.copyOf(byId.values());
    }

    @Override
    public int count() {
        return byId.size();
    }

    @Override
    public void clear() {
        byId.clear();
        byName.clear();
    }

    public Optional<Role> findByName(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    public List<Role> findByFilter(RoleFilter filter) {
        Objects.requireNonNull(filter, "Фильтр не может быть null");
        return byId.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        Objects.requireNonNull(filter, "Фильтр не может быть null");
        Objects.requireNonNull(sorter, "Сортировщик не может быть null");
        return byId.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public boolean exists(String name) {
        return byName.containsKey(name);
    }

    public void update(String roleName, String newName, String newDescription) {
        Role role = getExistingByName(roleName);
        if (!roleName.equals(newName) && byName.containsKey(newName)) {
            throw new IllegalStateException("Роль с именем '%s' уже существует".formatted(newName));
        }
        byName.remove(roleName);
        role.update(newName, newDescription);
        byName.put(newName, role);
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        Objects.requireNonNull(permission, "Permission не может быть null");
        getExistingByName(roleName).addPermission(permission);
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        Objects.requireNonNull(permission, "Permission не может быть null");
        getExistingByName(roleName).removePermission(permission);
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        Objects.requireNonNull(permissionName, "Имя разрешения не может быть null");
        Objects.requireNonNull(resource, "Ресурс не может быть null");

        return byId.values().stream()
                .filter(r -> r.hasPermission(permissionName, resource))
                .collect(Collectors.toList());
    }

    private Role getExistingByName(String roleName) {
        Objects.requireNonNull(roleName, "Имя роли не может быть null");
        Role role = byName.get(roleName);
        if (role == null) {
            throw new NoSuchElementException(
                    "Роль с именем '%s' не найдена".formatted(roleName));
        }
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoleManager that)) return false;
        return byId.equals(that.byId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(byId);
    }

    @Override
    public String toString() {
        return "RoleManager{count=%d}".formatted(byId.size());
    }
}
