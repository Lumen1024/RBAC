package org.example.command;

import org.example.RBACSystem;

import java.util.List;
import java.util.Scanner;

@FunctionalInterface
public interface CommandAction {
    void execute(Scanner scanner, RBACSystem rbacSystem, List<String> args);
}
