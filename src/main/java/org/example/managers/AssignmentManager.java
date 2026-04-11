package org.example.managers;

import org.example.assignment.RoleAssignment;
import org.example.assignment.TemporaryAssignment;
import org.example.data.Permission;
import org.example.data.Role;
import org.example.data.User;
import org.example.filter.AssignmentFilter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AssignmentManager implements Repository<RoleAssignment> {

    private final Map<String, RoleAssignment> assignments = new ConcurrentHashMap<>();
    private final UserManager userManager;
    private final RoleManager roleManager;

    public AssignmentManager(UserManager userManager, RoleManager roleManager) {
        Objects.requireNonNull(userManager, "UserManager не может быть null");
        Objects.requireNonNull(roleManager, "RoleManager не может быть null");
        this.userManager = userManager;
        this.roleManager = roleManager;
    }

    @Override
    public synchronized void add(RoleAssignment item) {
        Objects.requireNonNull(item, "Назначение не может быть null");

        if (userManager.findById(item.user().username()).isEmpty()) {
            throw new NoSuchElementException(
                    "Пользователь '%s' не найден".formatted(item.user().username()));
        }

        if (roleManager.findById(item.role().getId()).isEmpty()) {
            throw new NoSuchElementException(
                    "Роль '%s' не найдена".formatted(item.role().getName()));
        }

        boolean hasDuplicate = assignments.values().stream()
                .anyMatch(a -> a.user().equals(item.user())
                        && a.role().equals(item.role())
                        && a.isActive());
        if (hasDuplicate) {
            throw new IllegalStateException(
                    "Пользователь '%s' уже имеет активное назначение роли '%s'"
                            .formatted(item.user().username(), item.role().getName()));
        }

        assignments.put(item.assignmentId(), item);
    }

    @Override
    public boolean remove(RoleAssignment item) {
        if (item == null) return false;
        return assignments.remove(item.assignmentId(), item);
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        return Optional.ofNullable(assignments.get(id));
    }

    @Override
    public List<RoleAssignment> findAll() {
        return List.copyOf(assignments.values());
    }

    @Override
    public int count() {
        return assignments.size();
    }

    @Override
    public void clear() {
        assignments.clear();
    }


    public List<RoleAssignment> findByUser(User user) {
        Objects.requireNonNull(user, "Пользователь не может быть null");
        return assignments.values().stream()
                .filter(a -> a.user().equals(user))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByRole(Role role) {
        Objects.requireNonNull(role, "Роль не может быть null");
        return assignments.values().stream()
                .filter(a -> a.role().equals(role))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        Objects.requireNonNull(filter, "Фильтр не может быть null");
        return assignments.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        Objects.requireNonNull(filter, "Фильтр не может быть null");
        Objects.requireNonNull(sorter, "Сортировщик не может быть null");
        return assignments.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getActiveAssignments() {
        return assignments.values().stream()
                .filter(RoleAssignment::isActive)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getExpiredAssignments() {
        return assignments.values().stream()
                .filter(a -> !a.isActive())
                .collect(Collectors.toList());
    }

    public boolean userHasRole(User user, Role role) {
        Objects.requireNonNull(user, "Пользователь не может быть null");
        Objects.requireNonNull(role, "Роль не может быть null");
        return assignments.values().stream()
                .anyMatch(a -> a.user().equals(user) && a.role().equals(role) && a.isActive());
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        Objects.requireNonNull(user, "Пользователь не может быть null");
        Objects.requireNonNull(permissionName, "permissionName не может быть null");
        Objects.requireNonNull(resource, "resource не может быть null");

        return assignments.values().stream()
                .filter(a -> a.user().equals(user) && a.isActive())
                .anyMatch(a -> a.role().hasPermission(permissionName, resource));
    }

    public Set<Permission> getUserPermissions(User user) {
        Objects.requireNonNull(user, "Пользователь не может быть null");
        return assignments.values().stream()
                .filter(a -> a.user().equals(user) && a.isActive())
                .flatMap(a -> a.role().getPermissions().stream())
                .collect(Collectors.toSet());
    }

    public synchronized void revokeAssignment(String assignmentId) {
        Objects.requireNonNull(assignmentId, "assignmentId не может быть null");
        if (!assignments.containsKey(assignmentId))
            throw new NoSuchElementException("Назначение с ID '%s' не найдено".formatted(assignmentId));

        assignments.remove(assignmentId);
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        Objects.requireNonNull(assignmentId, "assignmentId не может быть null");
        Objects.requireNonNull(newExpirationDate, "newExpirationDate не может быть null");

        RoleAssignment assignment = assignments.get(assignmentId);
        if (assignment == null) {
            throw new NoSuchElementException(
                    "Назначение с ID '%s' не найдено".formatted(assignmentId));
        }
        if (!(assignment instanceof TemporaryAssignment temp)) {
            throw new IllegalStateException(
                    "Назначение '%s' не является временным".formatted(assignmentId));
        }
        temp.extend(newExpirationDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssignmentManager that)) return false;
        return assignments.equals(that.assignments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignments);
    }

    @Override
    public String toString() {
        return "AssignmentManager{count=%d}".formatted(assignments.size());
    }
}
