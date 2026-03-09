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

        StringBuilder sb = new StringBuilder();
        sb.append("=== Отчёт по пользователям ===\n");

        List<User> users = userManager.findAll().stream()
                .sorted(Comparator.comparing(User::username))
                .toList();

        if (users.isEmpty()) {
            sb.append("Пользователи отсутствуют.\n");
            return sb.toString();
        }

        for (User user : users) {
            sb.append("\nПользователь: ").append(user.format()).append("\n");

            List<RoleAssignment> active = assignmentManager.findByUser(user).stream()
                    .filter(RoleAssignment::isActive)
                    .toList();

            if (active.isEmpty()) {
                sb.append("  Роли: нет активных назначений\n");
            } else {
                sb.append("  Роли (").append(active.size()).append("):\n");
                for (RoleAssignment a : active) {
                    sb.append("    - ").append(a.role().getName())
                            .append(" [").append(a.assignmentType()).append("]\n");
                }
            }
        }

        sb.append("\nВсего пользователей: ").append(users.size()).append("\n");
        return sb.toString();
    }

    public String generateRoleReport() {
        var roleManager = system.getRoleManager();
        var assignmentManager = system.getAssignmentManager();

        StringBuilder sb = new StringBuilder();
        sb.append("=== Отчёт по ролям ===\n");

        List<Role> roles = roleManager.findAll().stream()
                .sorted(Comparator.comparing(Role::getName))
                .toList();

        if (roles.isEmpty()) {
            sb.append("Роли отсутствуют.\n");
            return sb.toString();
        }

        for (Role role : roles) {
            long userCount = assignmentManager.findByRole(role).stream()
                    .filter(RoleAssignment::isActive)
                    .map(a -> a.user().username())
                    .distinct()
                    .count();

            sb.append("\nРоль: ").append(role.getName()).append("\n");
            sb.append("  Описание: ").append(role.getDescription()).append("\n");
            sb.append("  Пользователей: ").append(userCount).append("\n");
            sb.append("  Разрешений: ").append(role.getPermissions().size()).append("\n");

            role.getPermissions().stream()
                    .sorted(Comparator.comparing(Permission::name).thenComparing(Permission::resource))
                    .forEach(p -> sb.append("    - ").append(p.format()).append("\n"));
        }

        sb.append("\nВсего ролей: ").append(roles.size()).append("\n");
        return sb.toString();
    }

    public String generatePermissionMatrix() {
        var userManager = system.getUserManager();
        var assignmentManager = system.getAssignmentManager();

        StringBuilder sb = new StringBuilder();
        sb.append("=== Матрица прав (пользователи × ресурсы) ===\n");

        List<User> users = userManager.findAll().stream()
                .sorted(Comparator.comparing(User::username))
                .toList();

        if (users.isEmpty()) {
            sb.append("Пользователи отсутствуют.\n");
            return sb.toString();
        }

        List<String> resources = users.stream()
                .flatMap(u -> assignmentManager.getUserPermissions(u).stream())
                .map(Permission::resource)
                .distinct()
                .sorted()
                .toList();

        if (resources.isEmpty()) {
            sb.append("Разрешения не назначены.\n");
            return sb.toString();
        }

        int userColWidth = Math.max(
                users.stream().mapToInt(u -> u.username().length()).max().orElse(8),
                12
        );
        int resColWidth = Math.max(
                resources.stream().mapToInt(String::length).max().orElse(8),
                10
        );

        sb.append(String.format("%-" + userColWidth + "s", "Пользователь"));
        for (String resource : resources) {
            sb.append(" | ").append(String.format("%-" + resColWidth + "s", resource));
        }
        sb.append("\n");
        sb.append("-".repeat(userColWidth + (resColWidth + 3) * resources.size())).append("\n");

        for (User user : users) {
            Set<Permission> perms = assignmentManager.getUserPermissions(user);
            sb.append(String.format("%-" + userColWidth + "s", user.username()));

            for (String resource : resources) {
                String actions = perms.stream()
                        .filter(p -> p.resource().equals(resource))
                        .map(Permission::name)
                        .sorted()
                        .collect(Collectors.joining(","));
                sb.append(" | ").append(String.format("%-" + resColWidth + "s", actions.isEmpty() ? "-" : actions));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    public void exportToFile(String report, String filename) {
        try (PrintWriter writer = new PrintWriter(filename)) {
            writer.print(report);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить отчёт в файл '%s': %s".formatted(filename, e.getMessage()), e);
        }
    }
}