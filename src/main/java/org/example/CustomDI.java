package org.example;

import org.example.command.CommandParser;
import org.example.utils.AuditLog;
import org.example.managers.AssignmentManager;
import org.example.managers.RoleManager;
import org.example.managers.UserManager;

public class CustomDI {
    private final static String CURRENT_USER = "Misha";

    private static UserManager userManager = null;
    private static RoleManager roleManager = null;
    private static AssignmentManager assignmentManager = null;
    private static RBACSystem rbacSystem = null;
    private static CommandParser commandParser = null;
    private static AuditLog logger = null;


    public static synchronized UserManager getUserManager() {
        if (userManager == null)
            userManager = new UserManager();

        return userManager;
    }

    public static synchronized RoleManager getRoleManager() {
        if (roleManager == null)
            roleManager = new RoleManager();

        return roleManager;
    }

    public static synchronized AssignmentManager getAssignmentManager() {
        if (assignmentManager == null)
            assignmentManager = new AssignmentManager(getUserManager(), getRoleManager());

        return assignmentManager;
    }

    public static synchronized RBACSystem getRbacSystem() {
        if (rbacSystem == null)
            rbacSystem = new RBACSystem(getUserManager(), getRoleManager(), getAssignmentManager(), CURRENT_USER);

        return rbacSystem;
    }

    public static synchronized CommandParser getCommandParser() {
        if (commandParser == null)
            commandParser =  new CommandParser(getRbacSystem());

        return commandParser;
    }

    public static synchronized AuditLog getLogger() {
        if (logger == null)
            logger =  new AuditLog();

        return logger;
    }


}
