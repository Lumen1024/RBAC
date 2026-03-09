package org.example.command;

import org.example.RBACSystem;
import org.example.utils.ValidationUtils;

import java.util.*;

public class CommandParser {

    private final List<Command> commands = new ArrayList<>();
    private final RBACSystem rbacSystem;

    public CommandParser(RBACSystem rbacSystem) {
        this.rbacSystem = rbacSystem;
    }

    public static Optional<ArgumentSet> parseArgs(List<String> args, Map<String, Integer> flags_signature) {
        var flags = new HashMap<String, List<String>>();
        var base_args = new ArrayList<String>();

        for (int i = 0; i < args.size(); i++) {
            if (flags_signature.containsKey(args.get(i))) {
                var flag_name = args.get(i);
                var flag_values_count = flags_signature.get(flag_name);

                var flag_values = new ArrayList<String>();

                if (i + flag_values_count > args.size())
                    return Optional.empty();

                for (int j = i + 1; j <= i + flag_values_count; j++) {
                    flag_values.add(args.get(j));
                }

                flags.put(flag_name, flag_values);
                i += flag_values_count;
                continue;
            }
            base_args.add(args.get(i));
        }

        return Optional.of(new ArgumentSet(flags, base_args));
    }

    void registerCommand(Command command) {
        commands.add(command);
    }

    public void executeCommand(String input, Scanner scanner) {
        ValidationUtils.requireNonEmpty(input, "input");
        var formatted_input = input.trim().replaceAll("\\s+", " ");
        var line = formatted_input.split(" ");

        var command = commands.stream()
                .filter(c -> c.getName().equals(line[0]))
                .findFirst()
                .orElse(null);

        if (command == null) {
            System.out.println("Нет такой команды");
            return;
        }

        var args = parseArgs(
                List.of(line).subList(1, line.length),
                command.getFlagsSignature()
        ).orElse(null);
        if (args == null) {
            System.out.println("Неправильный вызов команды");
            return;
        }
        command.execute(scanner, rbacSystem, args);

    }

    void printHelp(String command_name) {
        var command = commands.stream()
                .filter(c -> c.getName().equals(command_name))
                .findFirst()
                .orElse(null);

        if (command == null) {
            System.out.println("Нет такой команды");
            return;
        }

        System.out.println(command.getDescription());
    }

    void printHelp() {
        for (Command command : commands) {
            System.out.println(command.getName());

            for (String str : command.getDescription().trim().split("\n")) {
                System.out.println("\t" + str);
            }
        }
    }


}
