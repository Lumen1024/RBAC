package org.example.command;

import org.example.RBACSystem;

import java.util.List;
import java.util.Scanner;

public class Command {
    private final String name;
    private final String description;
    private final CommandAction action;

    public Command(String name, String description, CommandAction action) {
        this.name = name;
        this.description = description;
        this.action = action;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void execute(Scanner scanner, RBACSystem rbacSystem, List<String> args) {
        action.execute(scanner, rbacSystem, args);
    }

}
