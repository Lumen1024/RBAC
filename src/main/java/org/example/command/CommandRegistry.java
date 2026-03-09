package org.example.command;

import org.example.CustomDI;
import org.example.assignment.AssignmentMetadata;
import org.example.assignment.PermanentAssignment;
import org.example.assignment.RoleAssignment;
import org.example.assignment.TemporaryAssignment;
import org.example.data.Permission;
import org.example.data.Role;
import org.example.data.User;
import org.example.filter.UserFilters;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {

    public static void registerUserCommands(CommandParser parser) {
        parser.registerCommand(new Command(
                "user-list",
                """
                        Выводит список пользователей
                        flags:
                            --username <value> - содержит имя пользователя
                            --email <value> - содержит email
                            --domain <value> - содержит домен email
                            --fullname <value> - содержит фамилию""",
                Map.ofEntries(
                        Map.entry("--username", 1),
                        Map.entry("--email", 1),
                        Map.entry("--domain", 1),
                        Map.entry("--fullname", 1)
                ),
                0,
                (_, system, args) -> {

                    var users = system.getUserManager().findAll().stream()
                            .filter(u -> args.getFlagValue("--username")
                                    .map(v -> UserFilters.byUsername(v).test(u))
                                    .orElse(true))
                            .filter(u -> args.getFlagValue("--email")
                                    .map(v -> UserFilters.byEmail(v).test(u))
                                    .orElse(true))
                            .filter(u -> args.getFlagValue("--domain")
                                    .map(v -> UserFilters.byEmailDomain(v).test(u))
                                    .orElse(true))
                            .filter(u -> args.getFlagValue("--fullname")
                                    .map(v -> UserFilters.byFullNameContains(v).test(u))
                                    .orElse(true))
                            .toList();

                    for (User user : users) {
                        System.out.println(user.format());
                    }
                }
        ));
        parser.registerCommand(new Command(
                "user-create",
                """
                        Создает пользователя
                        usage: user-create <username> <fullname> <email>""",
                new HashMap<>(),
                3,
                (_, system, args) -> {
                    var username = args.baseArgs().getFirst();
                    var fullname = args.baseArgs().get(1);
                    var email = args.baseArgs().get(2);

                    try {
                        var user = User.create(username, fullname, email);
                        system.getUserManager().add(user);
                        CustomDI.getLogger().log("create user", system.getCurrentUser(), user.format(), null);
                    } catch (Exception e) {
                        System.out.println("Ошибка: " + e.getMessage());
                    }
                }
        ));
        parser.registerCommand(new Command(
                "user-view",
                """
                        Информация о пользователе
                        usage: user-view <username>""",
                (_, system, args) -> {
                    var username = args.baseArgs().getFirst();
                    system.getUserManager().findByUsername(username).ifPresentOrElse(
                            u -> System.out.println(u.format()),
                            () -> System.out.println("Пользователя не найдено")
                    );
                }
        ));
        parser.registerCommand(new Command(
                "user-update",
                """
                        Обновляет fullname и/или email пользователя
                        usage: user-update <flags> <username>
                        flags:
                            --fullname <value> - обновление fullname
                            --email <value> - обновление email""",
                Map.ofEntries(
                        Map.entry("--email", 1),
                        Map.entry("--fullname", 1)
                ),
                1,
                (_, system, args) -> {
                    var username = args.baseArgs().getFirst();
                    var fullname = args.getFlagValue("--fullname");
                    var email = args.getFlagValue("--email");

                    if (email.isEmpty() && fullname.isEmpty()) {
                        System.out.println("Не передано флагов для обновления полей");
                        return;
                    }

                    system.getUserManager().findByUsername(username).ifPresentOrElse(
                            u -> {
                                system.getUserManager().update(
                                        username,
                                        fullname.orElseGet(u::fullName),
                                        email.orElseGet(u::email));
                                CustomDI.getLogger().log(
                                        "update user",
                                        system.getCurrentUser(),
                                        u.format(),
                                        "by update-user"
                                );
                            }
                            ,
                            () -> System.out.println("Пользователя не найдено")
                    );
                }
        ));
        parser.registerCommand(new Command(
                "user-delete",
                """
                        Удаление пользователя и его назначений
                        usage: user-delete <username>""",
                new HashMap<>(),
                1,
                (scanner, system, args) -> {
                    var username = args.baseArgs().getFirst();
                    var user = system.getUserManager().findByUsername(username);
                    if (user.isEmpty()) {
                        System.out.println("Пользователя не найдено");
                        return;
                    }
                    System.out.printf("""
                            Точно хотите удалить пользователя:
                                %s
                            [Y/N]:
                            """, user.get().format());

                    if (scanner.next().equalsIgnoreCase("y")) {
                        CustomDI.getLogger().log(
                                "delete user", system.getCurrentUser(),
                                user.get().format(), "by user-delete"
                        );
                        for (var a : system.getAssignmentManager().findByUser(user.get()))
                            system.getAssignmentManager().remove(a);
                        system.getUserManager().remove(user.get());
                    }

                }
        ));
    }

    public static void registerRoleCommands(CommandParser parser) {
        parser.registerCommand(new Command(
                "role-list",
                """
                        Выводит список ролей
                        flags:
                            --name <value> - содержит имя роли
                            --permission <name> <resource> - имеет указанное право
                            --min-permissions <count> - минимальное количество прав""",
                Map.ofEntries(
                        Map.entry("--name", 1),
                        Map.entry("--permission", 2),
                        Map.entry("--min-permissions", 1)
                ),
                0,
                (_, system, args) -> {
                    var roles = system.getRoleManager().findAll().stream()
                            .filter(r -> args.getFlagValue("--name")
                                    .map(v -> r.getName().toLowerCase().contains(v.toLowerCase()))
                                    .orElse(true))
                            .filter(r -> args.getFlagValues("--permission")
                                    .map(v -> r.hasPermission(v.getFirst(), v.get(1)))
                                    .orElse(true))
                            .filter(r -> args.getFlagValue("--min-permissions")
                                    .map(v -> r.getPermissions().size() >= Integer.parseInt(v))
                                    .orElse(true))
                            .toList();

                    for (var role : roles) {
                        System.out.println(role.format());
                    }
                }
        ));

        parser.registerCommand(new Command(
                "role-create",
                """
                        Создает новую роль
                        usage: role-create <name> <description>""",
                new HashMap<>(),
                2,
                (_, system, args) -> {
                    var name = args.baseArgs().getFirst();
                    var description = args.baseArgs().get(1);

                    try {
                        var role = new Role(name, description);
                        system.getRoleManager().add(role);
                        System.out.println("Роль создана: " + role.getName());
                        System.out.println("Для добавления прав используйте: role-add-permission " + role.getName() + " <perm-name> <resource> <description>");
                        CustomDI.getLogger().log(
                                "create role", system.getCurrentUser(),
                                role.format(), "by role-create"
                        );
                    } catch (Exception e) {
                        System.out.println("Ошибка: " + e.getMessage());
                    }
                }
        ));

        parser.registerCommand(new Command(
                "role-view",
                """
                        Информация о роли
                        usage: role-view <name>""",
                (_, system, args) -> {
                    var name = args.baseArgs().getFirst();
                    system.getRoleManager().findByName(name).ifPresentOrElse(
                            r -> System.out.println(r.format()),
                            () -> System.out.println("Роль не найдена")
                    );
                }
        ));

        parser.registerCommand(new Command(
                "role-update",
                """
                        Обновляет name и/или description роли
                        usage: role-update <flags> <name>
                        flags:
                            --name <value> - новое имя роли
                            --description <value> - новое описание роли""",
                Map.ofEntries(
                        Map.entry("--name", 1),
                        Map.entry("--description", 1)
                ),
                1,
                (_, system, args) -> {
                    var roleName = args.baseArgs().getFirst();
                    var newName = args.getFlagValue("--name");
                    var newDescription = args.getFlagValue("--description");

                    if (newName.isEmpty() && newDescription.isEmpty()) {
                        System.out.println("Не передано флагов для обновления полей");
                        return;
                    }

                    system.getRoleManager().findByName(roleName).ifPresentOrElse(
                            r -> {
                                system.getRoleManager().update(
                                        roleName,
                                        newName.orElseGet(r::getName),
                                        newDescription.orElseGet(r::getDescription)
                                );
                                CustomDI.getLogger().log(
                                        "update role", system.getCurrentUser(),
                                        r.format(), "by role-update"
                                );
                            },
                            () -> System.out.println("Роль не найдена")
                    );
                }
        ));

        parser.registerCommand(new Command(
                "role-delete",
                """
                        Удаление роли и её назначений
                        usage: role-delete <name>""",
                new HashMap<>(),
                1,
                (scanner, system, args) -> {
                    var name = args.baseArgs().getFirst();
                    var role = system.getRoleManager().findByName(name).orElse(null);
                    if (role == null) {
                        System.out.println("Роль не найдена");
                        return;
                    }

                    var assignments = system.getAssignmentManager().findByRole(role);
                    if (!assignments.isEmpty()) {
                        System.out.println("Внимание! Роль назначена следующим пользователям:");
                        for (var a : assignments) {
                            System.out.println("  - " + a.user().format());
                        }
                    }

                    System.out.printf("""
                            Точно хотите удалить роль:
                                %s
                            [Y/N]:
                            """, role.getName());

                    if (scanner.next().equalsIgnoreCase("y")) {
                        CustomDI.getLogger().log(
                                "delete role", system.getCurrentUser(),
                                role.format(), "by role-delete"
                        );
                        for (var a : assignments) {
                            system.getAssignmentManager().remove(a);
                        }
                        system.getRoleManager().remove(role);
                    }
                }
        ));

        parser.registerCommand(new Command(
                "role-add-permission",
                """
                        Добавить право к роли
                        usage: role-add-permission <role-name> <perm-name> <resource> <description>""",
                new HashMap<>(),
                4,
                (_, system, args) -> {
                    var roleName = args.baseArgs().getFirst();
                    var permName = args.baseArgs().get(1);
                    var resource = args.baseArgs().get(2);
                    var description = args.baseArgs().get(3);

                    try {
                        var permission = new Permission(permName, resource, description);
                        system.getRoleManager().addPermissionToRole(roleName, permission);
                        System.out.println("Право добавлено: " + permission.format());
                        CustomDI.getLogger().log(
                                "add permission", system.getCurrentUser(),
                                roleName + ": " + permission.format(), "by role-add-permission"
                        );
                    } catch (Exception e) {
                        System.out.println("Ошибка: " + e.getMessage());
                    }
                }
        ));

        parser.registerCommand(new Command(
                "role-remove-permission",
                """
                        Удалить право из роли
                        usage: role-remove-permission <role-name>""",
                new HashMap<>(),
                1,
                (scanner, system, args) -> {
                    var roleName = args.baseArgs().getFirst();
                    var role = system.getRoleManager().findByName(roleName).orElse(null);
                    if (role == null) {
                        System.out.println("Роль не найдена");
                        return;
                    }

                    var permissions = role.getPermissions().stream().toList();
                    if (permissions.isEmpty()) {
                        System.out.println("У роли нет прав");
                        return;
                    }

                    System.out.println("Права роли " + role.getName() + ":");
                    for (int i = 0; i < permissions.size(); i++) {
                        System.out.printf("  %d. %s%n", i + 1, permissions.get(i).format());
                    }
                    System.out.println("Введите номер права для удаления:");

                    try {
                        int index = Integer.parseInt(scanner.next()) - 1;
                        if (index < 0 || index >= permissions.size()) {
                            System.out.println("Неверный номер");
                            return;
                        }
                        var permission = permissions.get(index);
                        system.getRoleManager().removePermissionFromRole(roleName, permission);
                        System.out.println("Право удалено: " + permission.format());
                        CustomDI.getLogger().log(
                                "remove permission", system.getCurrentUser(),
                                roleName + ": " + permission.format(), "by role-remove-permission"
                        );
                    } catch (NumberFormatException e) {
                        System.out.println("Введите корректный номер");
                    }
                }
        ));
    }

    public static void registerAssignmentCommands(CommandParser parser) {
        parser.registerCommand(new Command(
                "assign-role",
                """
                        Назначить роль пользователю
                        usage: assign-role <username> <role-name> <reason>
                        flags:
                            --temporary <date> - временное назначение до указанной даты (ISO: 2024-12-31T23:59:59)""",
                Map.of("--temporary", 1),
                3,
                (_, system, args) -> {
                    var username = args.baseArgs().getFirst();
                    var roleName = args.baseArgs().get(1);
                    var reason = args.baseArgs().get(2);

                    var user = system.getUserManager().findByUsername(username).orElse(null);
                    if (user == null) {
                        System.out.println("Пользователь не найден");
                        return;
                    }

                    var role = system.getRoleManager().findByName(roleName).orElse(null);
                    if (role == null) {
                        System.out.println("Роль не найдена");
                        return;
                    }

                    var metadata = AssignmentMetadata.now(system.getCurrentUser(), reason);
                    var expiresAt = args.getFlagValue("--temporary");

                    try {
                        RoleAssignment assignment = expiresAt.isPresent()
                                ? new TemporaryAssignment(user, role, metadata, expiresAt.get(), false)
                                : new PermanentAssignment(user, role, metadata);
                        system.getAssignmentManager().add(assignment);
                        System.out.printf("Роль «%s» назначена пользователю %s (%s)%n",
                                roleName, username, assignment.assignmentType());
                        CustomDI.getLogger().log(
                                "assign role", system.getCurrentUser(),
                                username + " -> " + roleName, "by assign-role"
                        );
                    } catch (Exception e) {
                        System.out.println("Ошибка: " + e.getMessage());
                    }
                }
        ));

        parser.registerCommand(new Command(
                "revoke-role",
                """
                        Отозвать роль у пользователя
                        usage: revoke-role <username> <role-name>""",
                new HashMap<>(),
                2,
                (_, system, args) -> {
                    var username = args.baseArgs().getFirst();
                    var roleName = args.baseArgs().get(1);

                    var user = system.getUserManager().findByUsername(username).orElse(null);
                    if (user == null) {
                        System.out.println("Пользователь не найден");
                        return;
                    }

                    var role = system.getRoleManager().findByName(roleName).orElse(null);
                    if (role == null) {
                        System.out.println("Роль не найдена");
                        return;
                    }

                    var assignment = system.getAssignmentManager().findAll().stream()
                            .filter(a -> a.user().equals(user) && a.role().equals(role) && a.isActive())
                            .findFirst()
                            .orElse(null);

                    if (assignment == null) {
                        System.out.println("Активное назначение не найдено");
                        return;
                    }

                    if (assignment instanceof PermanentAssignment perm) {
                        perm.revoke();
                    } else {
                        system.getAssignmentManager().remove(assignment);
                    }
                    CustomDI.getLogger().log(
                            "revoke role", system.getCurrentUser(),
                            username + " -> " + roleName, "by revoke-role"
                    );
                    System.out.println("Роль «" + roleName + "» отозвана у пользователя " + username);
                }
        ));

        parser.registerCommand(new Command(
                "assignment-list",
                """
                        Список всех назначений
                        flags:
                            --username <value> - фильтр по пользователю
                            --role <value> - фильтр по роли
                            --type <PERMANENT|TEMPORARY> - фильтр по типу
                            --status <active|inactive> - фильтр по статусу
                            --assigned-after <date> - назначённые после даты (ISO: 2024-01-01T00:00:00)
                            --expires-before <date> - истекающие до даты (ISO: 2024-12-31T23:59:59)""",
                Map.ofEntries(
                        Map.entry("--username", 1),
                        Map.entry("--role", 1),
                        Map.entry("--type", 1),
                        Map.entry("--status", 1),
                        Map.entry("--assigned-after", 1),
                        Map.entry("--expires-before", 1)
                ),
                (_, system, args) -> {
                    var assignments = system.getAssignmentManager().findAll().stream()
                            .filter(a -> args.getFlagValue("--username")
                                    .map(v -> a.user().username().toLowerCase().contains(v.toLowerCase()))
                                    .orElse(true))
                            .filter(a -> args.getFlagValue("--role")
                                    .map(v -> a.role().getName().toLowerCase().contains(v.toLowerCase()))
                                    .orElse(true))
                            .filter(a -> args.getFlagValue("--type")
                                    .map(v -> a.assignmentType().equalsIgnoreCase(v))
                                    .orElse(true))
                            .filter(a -> args.getFlagValue("--status")
                                    .map(v -> switch (v.toLowerCase()) {
                                        case "active" -> a.isActive();
                                        case "inactive" -> !a.isActive();
                                        default -> true;
                                    })
                                    .orElse(true))
                            .filter(a -> args.getFlagValue("--assigned-after")
                                    .map(v -> LocalDateTime.parse(a.metadata().assignedAt())
                                            .isAfter(LocalDateTime.parse(v)))
                                    .orElse(true))
                            .filter(a -> args.getFlagValue("--expires-before")
                                    .map(v -> a instanceof TemporaryAssignment temp
                                            && LocalDateTime.parse(temp.getExpiresAt())
                                            .isBefore(LocalDateTime.parse(v)))
                                    .orElse(true))
                            .toList();

                    if (assignments.isEmpty()) {
                        System.out.println("Назначения не найдены");
                        return;
                    }

                    System.out.printf("%-20s | %-20s | %-9s | %-8s | %s%n",
                            "USERNAME", "ROLE", "TYPE", "STATUS", "ASSIGNED AT");
                    System.out.println("-".repeat(85));
                    for (var a : assignments) {
                        System.out.printf("%-20s | %-20s | %-9s | %-8s | %s%n",
                                a.user().username(),
                                a.role().getName(),
                                a.assignmentType(),
                                a.isActive() ? "ACTIVE" : "INACTIVE",
                                a.metadata().assignedAt());
                    }
                }
        ));

        parser.registerCommand(new Command(
                "assignment-extend",
                """
                        Продлить временное назначение
                        usage: assignment-extend <username> <role-name> <new-date>
                        date format: ISO (например: 2024-12-31T23:59:59)""",
                new HashMap<>(),
                3,
                (_, system, args) -> {
                    var username = args.baseArgs().getFirst();
                    var roleName = args.baseArgs().get(1);
                    var newDate = args.baseArgs().get(2);

                    var user = system.getUserManager().findByUsername(username).orElse(null);
                    if (user == null) {
                        System.out.println("Пользователь не найден");
                        return;
                    }

                    var role = system.getRoleManager().findByName(roleName).orElse(null);
                    if (role == null) {
                        System.out.println("Роль не найдена");
                        return;
                    }

                    var assignment = system.getAssignmentManager().findAll().stream()
                            .filter(a -> a.user().equals(user) && a.role().equals(role)
                                    && a instanceof TemporaryAssignment)
                            .findFirst()
                            .orElse(null);

                    if (assignment == null) {
                        System.out.println("Временное назначение не найдено");
                        return;
                    }

                    try {
                        system.getAssignmentManager().extendTemporaryAssignment(assignment.assignmentId(), newDate);
                        System.out.println("Назначение продлено до: " + newDate);
                        CustomDI.getLogger().log(
                                "extend assignment", system.getCurrentUser(),
                                username + " -> " + roleName, "by assignment-extend"
                        );
                    } catch (Exception e) {
                        System.out.println("Ошибка: " + e.getMessage());
                    }
                }
        ));
    }


    public static void registerPermissionCommands(CommandParser parser) {
        parser.registerCommand(new Command(
                "permissions-user",
                """
                        Все права конкретного пользователя, сгруппированные по ресурсам
                        usage: permissions-user <username>""",
                new HashMap<>(),
                1,
                (_, system, args) -> {
                    var username = args.baseArgs().getFirst();
                    var user = system.getUserManager().findByUsername(username).orElse(null);
                    if (user == null) {
                        System.out.println("Пользователь не найден");
                        return;
                    }

                    var permissions = system.getAssignmentManager().getUserPermissions(user);
                    if (permissions.isEmpty()) {
                        System.out.println("У пользователя нет прав");
                        return;
                    }

                    var grouped = new java.util.TreeMap<String, java.util.List<Permission>>();
                    for (var p : permissions) {
                        grouped.computeIfAbsent(p.resource(), _ -> new java.util.ArrayList<>()).add(p);
                    }

                    System.out.println("Права пользователя " + username + ":");
                    for (var entry : grouped.entrySet()) {
                        System.out.println("  [" + entry.getKey() + "]");
                        for (var p : entry.getValue()) {
                            System.out.println("    - " + p.name() + ": " + p.description());
                        }
                    }
                }
        ));

        parser.registerCommand(new Command(
                "permissions-check",
                """
                        Проверить, есть ли у пользователя конкретное право
                        usage: permissions-check <username> <permission-name> <resource>""",
                new HashMap<>(),
                3,
                (_, system, args) -> {
                    var username = args.baseArgs().getFirst();
                    var permName = args.baseArgs().get(1);
                    var resource = args.baseArgs().get(2);

                    var user = system.getUserManager().findByUsername(username).orElse(null);
                    if (user == null) {
                        System.out.println("Пользователь не найден");
                        return;
                    }

                    boolean hasPermission = system.getAssignmentManager().userHasPermission(user, permName, resource);
                    if (hasPermission) {
                        var sourceRole = system.getAssignmentManager().findByUser(user).stream()
                                .filter(a -> a.isActive() && a.role().hasPermission(permName, resource))
                                .findFirst()
                                .map(a -> a.role().getName())
                                .orElse("неизвестно");
                        System.out.printf("✓ Пользователь %s ИМЕЕТ право %s on %s (из роли: %s)%n",
                                username, permName.toUpperCase(), resource.toLowerCase(), sourceRole);
                    } else {
                        System.out.printf("✗ Пользователь %s НЕ ИМЕЕТ права %s on %s%n",
                                username, permName.toUpperCase(), resource.toLowerCase());
                    }
                }
        ));
    }

    public static void registerAdditionalCommands(CommandParser parser) {
        parser.registerCommand(new Command(
                "help",
                """
                        Справка по команде
                        usage: help <command-name?>""",
                new HashMap<>(),
                0,
                (_, system, args) -> {
                    if (args.baseArgs().isEmpty()) {
                        parser.printHelp();
                    } else {
                        parser.printHelp(args.baseArgs().get(0));
                    }
                }
        ));

        parser.registerCommand(new Command(
                "stats",
                """
                        Статистика системы
                        usage: stats""",
                new HashMap<>(),
                0,
                (_, system, _) -> {
                    System.out.println(system.generateStatistics());
                }
        ));
        parser.registerCommand(new Command(
                "clear",
                """
                        Очистка терминала
                        usage: clear""",
                new HashMap<>(),
                0,
                (_, system, _) -> {
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                }
        ));
        // todo: exit
    }
}
