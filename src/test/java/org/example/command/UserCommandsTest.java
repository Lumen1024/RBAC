package org.example.command;

import org.example.RBACSystem;
import org.example.data.User;
import org.example.managers.AssignmentManager;
import org.example.managers.RoleManager;
import org.example.managers.UserManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class UserCommandsTest {

    private CommandParser parser;
    private RBACSystem system;
    private ByteArrayOutputStream out;

    @BeforeEach
    void setUp() {
        var userManager = new UserManager();
        var roleManager = new RoleManager();
        var assignmentManager = new AssignmentManager(userManager, roleManager);
        system = new RBACSystem(userManager, roleManager, assignmentManager, "admin");
        parser = new CommandParser(system);
        CommandRegistry.registerUserCommands(parser);

        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
    }

    private String output() {
        return out.toString();
    }

    private Scanner scannerWith(String input) {
        return new Scanner(new ByteArrayInputStream(input.getBytes()));
    }

    // region user-create

    @Test
    void userCreate_createsUser() {
        parser.executeCommand("user-create alice Alice alice@mail.com", scannerWith(""));
        assertTrue(system.getUserManager().findByUsername("alice").isPresent());
    }

    @Test
    void userCreate_duplicateUsername_printsError() {
        parser.executeCommand("user-create alice Alice alice@mail.com", scannerWith(""));
        parser.executeCommand("user-create alice Other other@mail.com", scannerWith(""));
        assertTrue(output().contains("Ошибка"));
        assertEquals(1, system.getUserManager().count());
    }

    // endregion

    // region user-list

    @Test
    void userList_noFilter_listsAllUsers() {
        system.getUserManager().add(User.create("alice", "Alice A", "alice@mail.com"));
        system.getUserManager().add(User.create("bob", "Bob B", "bob@mail.com"));

        parser.executeCommand("user-list", scannerWith(""));

        assertTrue(output().contains("alice"));
        assertTrue(output().contains("bob"));
    }

    @Test
    void userList_filterByUsername_returnsMatching() {
        system.getUserManager().add(User.create("alice", "Alice A", "alice@mail.com"));
        system.getUserManager().add(User.create("bob", "Bob B", "bob@mail.com"));

        parser.executeCommand("user-list --username alice", scannerWith(""));

        assertTrue(output().contains("alice"));
        assertFalse(output().contains("bob"));
    }

    @Test
    void userList_filterByEmail_returnsMatching() {
        system.getUserManager().add(User.create("alice", "Alice A", "alice@mail.com"));
        system.getUserManager().add(User.create("bob", "Bob B", "bob@example.com"));

        parser.executeCommand("user-list --email alice@mail.com", scannerWith(""));

        assertTrue(output().contains("alice"));
        assertFalse(output().contains("bob"));
    }

    @Test
    void userList_filterByDomain_returnsMatching() {
        system.getUserManager().add(User.create("alice", "Alice A", "alice@mail.com"));
        system.getUserManager().add(User.create("bob", "Bob B", "bob@example.com"));

        parser.executeCommand("user-list --domain example.com", scannerWith(""));

        assertFalse(output().contains("alice"));
        assertTrue(output().contains("bob"));
    }

    @Test
    void userList_filterByFullname_returnsMatching() {
        system.getUserManager().add(User.create("alice", "Alice Smith", "alice@mail.com"));
        system.getUserManager().add(User.create("bob", "Bob Jones", "bob@mail.com"));

        parser.executeCommand("user-list --fullname Smith", scannerWith(""));

        assertTrue(output().contains("alice"));
        assertFalse(output().contains("bob"));
    }

    // endregion

    // region user-view

    @Test
    void userView_existingUser_printsInfo() {
        system.getUserManager().add(User.create("alice", "Alice A", "alice@mail.com"));
        parser.executeCommand("user-view alice", scannerWith(""));
        assertTrue(output().contains("alice"));
    }

    @Test
    void userView_unknownUser_printsNotFound() {
        parser.executeCommand("user-view nobody", scannerWith(""));
        assertTrue(output().contains("не найдено") || output().contains("не найден"));
    }

    // endregion

    // region user-update

    @Test
    void userUpdate_fullname_updatesSuccessfully() {
        system.getUserManager().add(User.create("alice", "AliceOld", "alice@mail.com"));
        parser.executeCommand("user-update --fullname AliceNew alice", scannerWith(""));
        var user = system.getUserManager().findByUsername("alice").orElseThrow();
        assertEquals("AliceNew", user.fullName());
    }

    @Test
    void userUpdate_email_updatesSuccessfully() {
        system.getUserManager().add(User.create("alice", "Alice A", "old@mail.com"));
        parser.executeCommand("user-update --email new@mail.com alice", scannerWith(""));
        var user = system.getUserManager().findByUsername("alice").orElseThrow();
        assertEquals("new@mail.com", user.email());
    }

    @Test
    void userUpdate_noFlags_printsError() {
        system.getUserManager().add(User.create("alice", "Alice A", "alice@mail.com"));
        parser.executeCommand("user-update alice", scannerWith(""));
        assertTrue(output().contains("флагов"));
    }

    @Test
    void userUpdate_unknownUser_printsNotFound() {
        parser.executeCommand("user-update --email x@y.com nobody", scannerWith(""));
        assertTrue(output().contains("не найдено") || output().contains("не найден"));
    }

    // endregion

    // region user-delete

    @Test
    void userDelete_confirmedYes_deletesUser() {
        system.getUserManager().add(User.create("alice", "Alice A", "alice@mail.com"));
        parser.executeCommand("user-delete alice", scannerWith("y"));
        assertTrue(system.getUserManager().findByUsername("alice").isEmpty());
    }

    @Test
    void userDelete_confirmedNo_keepsUser() {
        system.getUserManager().add(User.create("alice", "Alice A", "alice@mail.com"));
        parser.executeCommand("user-delete alice", scannerWith("n"));
        assertTrue(system.getUserManager().findByUsername("alice").isPresent());
    }

    @Test
    void userDelete_unknownUser_printsNotFound() {
        parser.executeCommand("user-delete nobody", scannerWith("y"));
        assertTrue(output().contains("не найдено") || output().contains("не найден"));
    }

    // endregion

    // region unknown command

    @Test
    void executeCommand_unknownCommand_printsError() {
        parser.executeCommand("nonexistent-cmd", scannerWith(""));
        assertTrue(output().contains("Нет такой команды"));
    }

    // endregion
}