package org.example.command;

import org.example.Permission;
import org.example.RBACSystem;
import org.example.Role;
import org.example.User;
import org.example.assignment.AssignmentMetadata;
import org.example.assignment.PermanentAssignment;
import org.example.assignment.TemporaryAssignment;
import org.example.managers.AssignmentManager;
import org.example.managers.RoleManager;
import org.example.managers.UserManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class AssignmentCommandsTest {

    private CommandParser parser;
    private RBACSystem system;
    private ByteArrayOutputStream out;

    private User alice;
    private Role admin;

    @BeforeEach
    void setUp() {
        var userManager = new UserManager();
        var roleManager = new RoleManager();
        var assignmentManager = new AssignmentManager(userManager, roleManager);
        system = new RBACSystem(userManager, roleManager, assignmentManager, "testAdmin");
        parser = new CommandParser(system);
        CommandRegistry.registerAssignmentCommands(parser);
        CommandRegistry.registerPermissionCommands(parser);

        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        alice = User.create("alice", "Alice A", "alice@mail.com");
        admin = new Role("Admin", "Администратор");
        admin.addPermission(new Permission("READ", "Users", "чтение"));

        system.getUserManager().add(alice);
        system.getRoleManager().add(admin);
    }

    private String output() {
        return out.toString();
    }

    private Scanner scannerWith(String input) {
        return new Scanner(new ByteArrayInputStream(input.getBytes()));
    }

    private String futureDate() {
        return LocalDateTime.now().plusYears(1).toString();
    }

    // region assign-role

    @Test
    void assignRole_permanent_createsAssignment() {
        parser.executeCommand("assign-role alice Admin причина", scannerWith(""));
        assertEquals(1, system.getAssignmentManager().count());
    }

    @Test
    void assignRole_permanent_printsPermanentType() {
        parser.executeCommand("assign-role alice Admin причина", scannerWith(""));
        assertTrue(output().contains("PERMANENT"));
    }

    @Test
    void assignRole_temporary_createsTemporaryAssignment() {
        String date = futureDate();
        parser.executeCommand("assign-role alice Admin причина --temporary " + date, scannerWith(""));

        var assignment = system.getAssignmentManager().findAll().getFirst();
        assertInstanceOf(TemporaryAssignment.class, assignment);
    }

    @Test
    void assignRole_unknownUser_printsError() {
        parser.executeCommand("assign-role nobody Admin причина", scannerWith(""));
        assertTrue(output().contains("не найден"));
        assertEquals(0, system.getAssignmentManager().count());
    }

    @Test
    void assignRole_unknownRole_printsError() {
        parser.executeCommand("assign-role alice NoRole причина", scannerWith(""));
        assertTrue(output().contains("не найдена") || output().contains("не найден"));
        assertEquals(0, system.getAssignmentManager().count());
    }

    // endregion

    // region revoke-role

    @Test
    void revokeRole_permanentAssignment_revokesIt() {
        var assignment = new PermanentAssignment(alice, admin, AssignmentMetadata.now("admin", "test"));
        system.getAssignmentManager().add(assignment);

        parser.executeCommand("revoke-role alice Admin", scannerWith(""));

        assertFalse(assignment.isActive());
    }

    @Test
    void revokeRole_temporaryAssignment_removesIt() {
        var assignment = new TemporaryAssignment(alice, admin,
                AssignmentMetadata.now("admin", "test"), futureDate(), false);
        system.getAssignmentManager().add(assignment);

        parser.executeCommand("revoke-role alice Admin", scannerWith(""));

        assertEquals(0, system.getAssignmentManager().count());
    }

    @Test
    void revokeRole_noActiveAssignment_printsError() {
        parser.executeCommand("revoke-role alice Admin", scannerWith(""));
        assertTrue(output().contains("не найдено") || output().contains("не найден"));
    }

    @Test
    void revokeRole_unknownUser_printsError() {
        parser.executeCommand("revoke-role nobody Admin", scannerWith(""));
        assertTrue(output().contains("не найден"));
    }

    @Test
    void revokeRole_unknownRole_printsError() {
        parser.executeCommand("revoke-role alice NoRole", scannerWith(""));
        assertTrue(output().contains("не найдена") || output().contains("не найден"));
    }

    // endregion

    // region assignment-list

    @Test
    void assignmentList_noFilter_listsAll() {
        system.getAssignmentManager().add(
                new PermanentAssignment(alice, admin, AssignmentMetadata.now("admin", "test"))
        );
        parser.executeCommand("assignment-list", scannerWith(""));
        assertTrue(output().contains("alice"));
        assertTrue(output().contains("Admin"));
    }

    @Test
    void assignmentList_empty_printsNotFound() {
        parser.executeCommand("assignment-list", scannerWith(""));
        assertTrue(output().contains("не найден"));
    }

    @Test
    void assignmentList_filterByUsername_returnsMatching() {
        var bob = User.create("bob", "Bob B", "bob@mail.com");
        system.getUserManager().add(bob);
        system.getAssignmentManager().add(
                new PermanentAssignment(alice, admin, AssignmentMetadata.now("admin", "test"))
        );
        system.getAssignmentManager().add(
                new PermanentAssignment(bob, admin, AssignmentMetadata.now("admin", "test"))
        );

        parser.executeCommand("assignment-list --username alice", scannerWith(""));
        assertTrue(output().contains("alice"));
        assertFalse(output().contains("bob"));
    }

    @Test
    void assignmentList_filterByType_returnsMatchingType() {
        system.getAssignmentManager().add(
                new PermanentAssignment(alice, admin, AssignmentMetadata.now("admin", "test"))
        );
        parser.executeCommand("assignment-list --type PERMANENT", scannerWith(""));
        assertTrue(output().contains("PERMANENT"));
    }

    @Test
    void assignmentList_filterByStatus_active_returnsActive() {
        system.getAssignmentManager().add(
                new PermanentAssignment(alice, admin, AssignmentMetadata.now("admin", "test"))
        );
        parser.executeCommand("assignment-list --status active", scannerWith(""));
        assertTrue(output().contains("ACTIVE"));
    }

    @Test
    void assignmentList_filterByRole_returnsMatching() {
        system.getAssignmentManager().add(
                new PermanentAssignment(alice, admin, AssignmentMetadata.now("admin", "test"))
        );
        parser.executeCommand("assignment-list --role Admin", scannerWith(""));
        assertTrue(output().contains("Admin"));
    }

    // endregion

    // region assignment-extend

    @Test
    void assignmentExtend_validTemporaryAssignment_extendsDate() {
        String originalDate = LocalDateTime.now().plusDays(1).toString();
        String newDate = LocalDateTime.now().plusYears(2).toString();

        var assignment = new TemporaryAssignment(alice, admin,
                AssignmentMetadata.now("admin", "test"), originalDate, false);
        system.getAssignmentManager().add(assignment);

        parser.executeCommand("assignment-extend alice Admin " + newDate, scannerWith(""));

        assertTrue(output().contains("продлено"));
        assertEquals(newDate, assignment.getExpiresAt());
    }

    @Test
    void assignmentExtend_noTemporaryAssignment_printsError() {
        parser.executeCommand("assignment-extend alice Admin 2030-01-01T00:00:00", scannerWith(""));
        assertTrue(output().contains("не найдено") || output().contains("не найден"));
    }

    @Test
    void assignmentExtend_unknownUser_printsError() {
        parser.executeCommand("assignment-extend nobody Admin 2030-01-01T00:00:00", scannerWith(""));
        assertTrue(output().contains("не найден"));
    }

    @Test
    void assignmentExtend_unknownRole_printsError() {
        parser.executeCommand("assignment-extend alice NoRole 2030-01-01T00:00:00", scannerWith(""));
        assertTrue(output().contains("не найдена") || output().contains("не найден"));
    }

    // endregion

    // region permissions-user

    @Test
    void permissionsUser_userWithRole_listsPermissions() {
        system.getAssignmentManager().add(
                new PermanentAssignment(alice, admin, AssignmentMetadata.now("admin", "test"))
        );
        parser.executeCommand("permissions-user alice", scannerWith(""));
        // Permission нормализует name в UPPER, resource в lower
        assertTrue(output().contains("READ"));
        assertTrue(output().contains("users"));
    }

    @Test
    void permissionsUser_userWithNoAssignments_printsNoPerms() {
        parser.executeCommand("permissions-user alice", scannerWith(""));
        assertTrue(output().contains("нет прав") || output().contains("нет"));
    }

    @Test
    void permissionsUser_unknownUser_printsError() {
        parser.executeCommand("permissions-user nobody", scannerWith(""));
        assertTrue(output().contains("не найден"));
    }

    // endregion

    // region permissions-check

    @Test
    void permissionsCheck_userHasPermission_printsConfirmation() {
        system.getAssignmentManager().add(
                new PermanentAssignment(alice, admin, AssignmentMetadata.now("admin", "test"))
        );
        parser.executeCommand("permissions-check alice READ Users", scannerWith(""));
        assertTrue(output().contains("ИМЕЕТ") || output().contains("✓"));
    }

    @Test
    void permissionsCheck_userLacksPermission_printsDenial() {
        system.getAssignmentManager().add(
                new PermanentAssignment(alice, admin, AssignmentMetadata.now("admin", "test"))
        );
        parser.executeCommand("permissions-check alice DELETE Games", scannerWith(""));
        assertTrue(output().contains("НЕ ИМЕЕТ") || output().contains("✗"));
    }

    @Test
    void permissionsCheck_unknownUser_printsError() {
        parser.executeCommand("permissions-check nobody READ Users", scannerWith(""));
        assertTrue(output().contains("не найден"));
    }

    // endregion
}