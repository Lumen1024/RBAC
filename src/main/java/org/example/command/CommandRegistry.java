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


    }
}
