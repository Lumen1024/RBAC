package org.example.command;

import org.example.Permission;
import org.example.Role;
import org.example.User;
import org.example.filter.UserFilters;

import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {

    private static void printArgumentCountError() {
        System.out.println("Неправильное количество аргументов");
    }

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



    }
}
