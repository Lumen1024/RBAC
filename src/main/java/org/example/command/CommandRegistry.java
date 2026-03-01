package org.example.command;

import org.example.User;
import org.example.filter.UserFilters;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class CommandRegistry {

    private static void printArgumentCountError() {
        System.out.println("Неправильное количество аргументов");
    }

    public static void registerCommands(CommandParser parser) {
        parser.registerCommand(new Command(
                "user-view",
                "Информация о пользователе\n" +
                        "usage: user-view <username>",
                (_, system, args) -> {
                    if (args.size() != 1) {
                        printArgumentCountError();
                        return;
                    }
                    var username = args.getFirst();
                    var user = system.getUserManager().findByUsername(username).orElse(null);
                    if (user != null) {
                        System.out.println(user.format());
                    } else {
                        System.out.println("Пользователя не найдено");
                    }
                }
        ));
        parser.registerCommand(new Command(
                "user-list",
                """
                        Выводит список пользователей
                        flags:
                        \t --username <value> - содержит имя пользователя
                        \t --email <value> - содержит email
                        \t --domain <value> - содержит домен email
                        \t --fullname <value> - содержит фамилию""",
                (_, system, args) -> {

                    var flag_sig = Map.ofEntries(
                            Map.entry("--username", 1),
                            Map.entry("--email", 1),
                            Map.entry("--domain", 1),
                            Map.entry("--fullname", 1)
                    );

                    var parsed_args = CommandParser.parseArgs(args, flag_sig).orElse(null);
                    if (parsed_args == null) {
                        printArgumentCountError();
                        return;
                    }

                    Function<String, Optional<String>> flag = key ->
                            Optional.ofNullable(parsed_args.flags().get(key)).map(List::getFirst);

                    var users = system.getUserManager().findAll().stream()
                            .filter(u -> flag.apply("--username").map(v -> UserFilters.byUsername(v).test(u)).orElse(true))
                            .filter(u -> flag.apply("--email").map(v -> UserFilters.byEmail(v).test(u)).orElse(true))
                            .filter(u -> flag.apply("--domain").map(v -> UserFilters.byEmailDomain(v).test(u)).orElse(true))
                            .filter(u -> flag.apply("--fullname").map(v -> UserFilters.byFullNameContains(v).test(u)).orElse(true))
                            .toList();

                    for (User user : users) {
                        System.out.println(user.format());
                    }
                }
        ));


    }
}
