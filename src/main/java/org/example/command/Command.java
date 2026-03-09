package org.example.command;

import org.example.RBACSystem;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Command {
    private final String name;
    private final String description;
    private final CommandAction action;

    private final Map<String, Integer> flagsSignature;
    private final int baseArgCount;

    public Command(String name, String description, CommandAction action) {
        this.name = name;
        this.description = description;
        this.action = action;
        this.baseArgCount = 0;
        flagsSignature = new HashMap<>();
    }

    public Command(
            String name,
            String description,
            Map<String, Integer> flags_signature,
            int baseArgCount,
            CommandAction action
    ) {
        this.name = name;
        this.description = description;
        this.action = action;
        this.baseArgCount = baseArgCount;
        this.flagsSignature = flags_signature;
    }

    public Command(
            String name,
            String description,
            Map<String, Integer> flags_signature,
            CommandAction action
    ) {
        this.name = name;
        this.description = description;
        this.action = action;
        this.baseArgCount = 0;
        this.flagsSignature = flags_signature;
    }



    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Integer> getFlagsSignature() {
        return flagsSignature;
    }

    public int getBaseArgCount() {
        return baseArgCount;
    }

    public void execute(Scanner scanner, RBACSystem rbacSystem, ArgumentSet args) {
        action.execute(scanner, rbacSystem, args);
    }

}
