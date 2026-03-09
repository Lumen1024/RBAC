package org.example;

import org.example.assignment.AssignmentMetadata;
import org.example.assignment.PermanentAssignment;
import org.example.managers.AssignmentManager;
import org.example.managers.RoleManager;
import org.example.managers.UserManager;

import java.util.Objects;

public class RBACSystem {

    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AssignmentManager assignmentManager;

    private String currentUser;

    public RBACSystem(UserManager userManager, RoleManager roleManager, AssignmentManager assignmentManager, String currentUser) {
        Objects.requireNonNull(userManager);
        Objects.requireNonNull(roleManager);
        Objects.requireNonNull(assignmentManager);

        this.userManager = userManager;
        this.roleManager = roleManager;
        this.assignmentManager = assignmentManager;
        this.currentUser = currentUser;
    }

    // region getters / setters

    public AssignmentManager getAssignmentManager() {
        return this.assignmentManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }

    // endregion

    public void init() {
        var read_users = new Permission("READ", "Users", "none");
        var write_users = new Permission("WRITE", "Users", "none");
        var delete_users = new Permission("DELETE", "Users", "none");
        var read_games = new Permission("READ", "Games", "none");
        var write_games = new Permission("WRITE", "Games", "none");
        var delete_games = new Permission("DELETE", "Games", "none");

        var admin = (new Role("Admin", "super puper"));
        admin.addPermission(read_users);
        admin.addPermission(write_users);
        admin.addPermission(delete_users);
        admin.addPermission(read_games);
        admin.addPermission(write_games);
        admin.addPermission(delete_games);
        roleManager.add(admin);

        var manager = new Role("Manager", "just me");
        manager.addPermission(read_users);
        manager.addPermission(read_games);
        manager.addPermission(write_games);
        manager.addPermission(delete_games);
        roleManager.add(manager);

        var viewer = new Role("Viewer", "just me");
        viewer.addPermission(read_users);
        viewer.addPermission(read_games);

        userManager.add(new User("Misha", "Belkov", "super@mail.ru"));

        assignmentManager.add(new PermanentAssignment(
                userManager.findByUsername("Misha").get(),
                roleManager.findByName("Admin").get(),
                AssignmentMetadata.now("me", "no reason")
        ));
    }

    public String generateStatistics() {
        return "Пользователей: %s, Ролей: %s, Назначений: %s".formatted(userManager.count(), roleManager.count(), assignmentManager.count());
    }


}
