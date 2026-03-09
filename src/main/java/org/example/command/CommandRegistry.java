package org.example.command;

import org.example.Permission;
import org.example.Role;
import org.example.User;
import org.example.assignment.AssignmentMetadata;
import org.example.assignment.PermanentAssignment;
import org.example.assignment.RoleAssignment;
import org.example.assignment.TemporaryAssignment;
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
                            u -> system.getUserManager().update(
                                    username,
                                    fullname.orElseGet(u::fullName),
                                    email.orElseGet(u::email)
                            ),
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
                            r -> system.getRoleManager().update(
                                    roleName,
                                    newName.orElseGet(r::getName),
                                    newDescription.orElseGet(r::getDescription)
                            ),
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
                    } catch (NumberFormatException e) {
                        System.out.println("Введите корректный номер");
                    }
                }
        ));
    }


    }
}
