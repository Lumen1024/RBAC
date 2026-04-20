package org.example.utils;

import org.example.RBACSystem;
import org.example.assignment.RoleAssignment;
import org.example.data.Permission;
import org.example.data.Role;
import org.example.data.User;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportGenerator {

    private final RBACSystem system;

    public ReportGenerator(RBACSystem system) {
        this.system = system;
    }

    public String generateUserReport() {
        var userManager = system.getUserManager();
        var assignmentManager = system.getAssignmentManager();

        List<User> users = userManager.findAll().stream()
                .sorted(Comparator.comparing(User::username))
                .toList();

        if (users.isEmpty()) return "Пользователи отсутствуют.";

        var headers = new String[]{"USERNAME", "FULLNAME", "EMAIL", "ROLES"};
        var rows = users.stream()
                .map(u -> {
                    String roles = assignmentManager.findByUser(u).stream()
                            .filter(RoleAssignment::isActive)
                            .map(a -> a.role().getName() + " [" + a.assignmentType() + "]")
                            .collect(Collectors.joining(", "));
                    return new String[]{u.username(), u.fullName(), u.email(), roles.isEmpty() ? "-" : roles};
                })
                .toList();

        return FormatUtils.formatTable("Отчёт по пользователям", headers, rows);
    }

    public String generateRoleReport() {
        var roleManager = system.getRoleManager();
        var assignmentManager = system.getAssignmentManager();

        List<Role> roles = roleManager.findAll().stream()
                .sorted(Comparator.comparing(Role::getName))
                .toList();

        if (roles.isEmpty()) return "Роли отсутствуют.";

        var headers = new String[]{"NAME", "DESCRIPTION", "USERS", "PERMISSIONS"};
        var rows = roles.stream()
                .map(r -> {
                    long userCount = assignmentManager.findByRole(r).stream()
                            .filter(RoleAssignment::isActive)
                            .map(a -> a.user().username())
                            .distinct()
                            .count();
                    String perms = r.getPermissions().stream()
                            .sorted(Comparator.comparing(Permission::name).thenComparing(Permission::resource))
                            .map(Permission::format)
                            .collect(Collectors.joining(", "));
                    return new String[]{r.getName(), r.getDescription(), String.valueOf(userCount), perms.isEmpty() ? "-" : perms};
                })
                .toList();

        return FormatUtils.formatTable("Отчёт по ролям", headers, rows);
    }

    public String generatePermissionMatrix() {
        var userManager = system.getUserManager();
        var assignmentManager = system.getAssignmentManager();

        List<User> users = userManager.findAll().stream()
                .sorted(Comparator.comparing(User::username))
                .toList();

        if (users.isEmpty()) return "Пользователи отсутствуют.";

        List<String> resources = users.stream()
                .flatMap(u -> assignmentManager.getUserPermissions(u).stream())
                .map(Permission::resource)
                .distinct()
                .sorted()
                .toList();

        if (resources.isEmpty()) return "Разрешения не назначены.";

        String[] headers = new String[resources.size() + 1];
        headers[0] = "USERNAME";
        for (int i = 0; i < resources.size(); i++)
            headers[i + 1] = resources.get(i).toUpperCase();

        List<String[]> rows = new ArrayList<>();
        for (User user : users) {
            Set<Permission> perms = assignmentManager.getUserPermissions(user);
            String[] row = new String[resources.size() + 1];
            row[0] = user.username();
            for (int i = 0; i < resources.size(); i++) {
                String resource = resources.get(i);
                String actions = perms.stream()
                        .filter(p -> p.resource().equals(resource))
                        .map(Permission::name)
                        .sorted()
                        .collect(Collectors.joining(","));
                row[i + 1] = actions.isEmpty() ? "-" : actions;
            }
            rows.add(row);
        }

        return FormatUtils.formatTable("Матрица прав", headers, rows);
    }

    public String generateUserReportAsync() {
        var userManager = system.getUserManager();
        var assignmentManager = system.getAssignmentManager();

        List<User> users = userManager.findAll().stream()
                .sorted(Comparator.comparing(User::username))
                .toList();

        if (users.isEmpty()) return "Пользователи отсутствуют.";

        var headers = new String[]{"USERNAME", "FULLNAME", "EMAIL", "ROLES"};
        var rows = users.parallelStream()
                .map(u -> {
                    String roles = assignmentManager.findByUser(u).stream()
                            .filter(RoleAssignment::isActive)
                            .map(a -> a.role().getName() + " [" + a.assignmentType() + "]")
                            .collect(Collectors.joining(", "));
                    return new String[]{u.username(), u.fullName(), u.email(), roles.isEmpty() ? "-" : roles};
                })
                .toList();

        return FormatUtils.formatTable("Отчёт по пользователям", headers, rows);
    }

    public String generatePermissionMatrixAsync() {
        var userManager = system.getUserManager();
        var assignmentManager = system.getAssignmentManager();

        List<User> users = userManager.findAll().stream()
                .sorted(Comparator.comparing(User::username))
                .toList();

        if (users.isEmpty()) return "Пользователи отсутствуют.";

        List<String> resources = users.parallelStream()
                .flatMap(u -> assignmentManager.getUserPermissions(u).stream())
                .map(Permission::resource)
                .distinct()
                .sorted()
                .toList();

        if (resources.isEmpty()) return "Разрешения не назначены.";

        String[] headers = new String[resources.size() + 1];
        headers[0] = "USERNAME";
        for (int i = 0; i < resources.size(); i++)
            headers[i + 1] = resources.get(i).toUpperCase();

        List<String[]> rows = users.parallelStream()
                .map(user -> {
                    Set<Permission> perms = assignmentManager.getUserPermissions(user);
                    String[] row = new String[resources.size() + 1];
                    row[0] = user.username();
                    for (int i = 0; i < resources.size(); i++) {
                        String resource = resources.get(i);
                        String actions = perms.stream()
                                .filter(p -> p.resource().equals(resource))
                                .map(Permission::name)
                                .sorted()
                                .collect(Collectors.joining(","));
                        row[i + 1] = actions.isEmpty() ? "-" : actions;
                    }
                    return row;
                })
                .toList();

        return FormatUtils.formatTable("Матрица прав", headers, rows);
    }

    public void exportToFile(String report, String filename) {
        try (PrintWriter writer = new PrintWriter(filename)) {
            writer.print(report);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить отчёт в файл '%s': %s".formatted(filename, e.getMessage()), e);
        }
    }
}