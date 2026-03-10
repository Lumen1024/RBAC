package org.example.utils;

import java.util.List;
import java.util.Scanner;

public class ConsoleUtils {

    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String CYAN   = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED    = "\u001B[31m";
    private static final String GREEN  = "\u001B[32m";

    public static String promptString(Scanner scanner, String message, boolean required) {
        while (true) {
            System.out.print(CYAN + BOLD + message + RESET + " ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                if (!required) return input;
                printError("Поле не может быть пустым. Попробуйте снова.");
            } else {
                return input;
            }
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(CYAN + BOLD + message + RESET + " [" + min + "-" + max + "]: ");
            String input = scanner.nextLine().trim();

            try {
                int value = Integer.parseInt(input);
                if (value < min || value > max) {
                    printError("Число должно быть в диапазоне [%d, %d].".formatted(min, max));
                } else {
                    return value;
                }
            } catch (NumberFormatException e) {
                printError("Введите целое число.");
            }
        }
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        while (true) {
            System.out.print(CYAN + BOLD + message + RESET + " [yes/no]: ");
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("yes") || input.equals("y")) return true;
            if (input.equals("no")  || input.equals("n")) return false;

            printError("Введите 'yes' или 'no'.");
        }
    }

    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        if (options == null || options.isEmpty())
            throw new IllegalArgumentException("Список вариантов не может быть пустым");

        System.out.println(BOLD + message + RESET);

        int numWidth = String.valueOf(options.size()).length();
        for (int i = 0; i < options.size(); i++) {
            System.out.println(
                    "  " + YELLOW + FormatUtils.padLeft(String.valueOf(i + 1), numWidth) + "." + RESET
                    + " " + options.get(i)
            );
        }

        int choice = promptInt(scanner, "Ваш выбор:", 1, options.size());
        T selected = options.get(choice - 1);
        System.out.println(GREEN + "Выбрано: " + selected + RESET);
        return selected;
    }

    // region helpers

    public static void printError(String message) {
        System.out.println(RED + "  [!] " + message + RESET);
    }

    public static void printSuccess(String message) {
        System.out.println(GREEN + "  [+] " + message + RESET);
    }

    public static void printInfo(String message) {
        System.out.println(CYAN + "  [i] " + message + RESET);
    }

    // endregion
}