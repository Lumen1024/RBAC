package org.example.command;

import org.example.data.Permission;
import org.example.RBACSystem;
import org.example.data.Role;
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

class RoleCommandsTest {

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
        CommandRegistry.registerRoleCommands(parser);

        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
    }

    private String output() {
        return out.toString();
    }

    private Scanner scannerWith(String input) {
        return new Scanner(new ByteArrayInputStream(input.getBytes()));
    }

    private Role roleWith(String name) {
        return new Role(name, "Описание " + name);
    }

    // region role-create

    @Test
    void roleCreate_createsRole() {
        parser.executeCommand("role-create Admin Администратор", scannerWith(""));
        assertTrue(system.getRoleManager().findByName("Admin").isPresent());
    }

    @Test
    void roleCreate_printSuccessMessage() {
        parser.executeCommand("role-create Admin Администратор", scannerWith(""));
        assertTrue(output().contains("Admin"));
    }

    @Test
    void roleCreate_duplicate_printsError() {
        system.getRoleManager().add(roleWith("Admin"));
        parser.executeCommand("role-create Admin Другое", scannerWith(""));
        assertTrue(output().contains("Ошибка"));
        assertEquals(1, system.getRoleManager().count());
    }

    // endregion

    // region role-list

    @Test
    void roleList_noFilter_listsAllRoles() {
        system.getRoleManager().add(roleWith("Admin"));
        system.getRoleManager().add(roleWith("Viewer"));

        parser.executeCommand("role-list", scannerWith(""));

        assertTrue(output().contains("Admin"));
        assertTrue(output().contains("Viewer"));
    }

    @Test
    void roleList_filterByName_returnsMatching() {
        system.getRoleManager().add(roleWith("Admin"));
        system.getRoleManager().add(roleWith("Viewer"));

        parser.executeCommand("role-list --name Admin", scannerWith(""));

        assertTrue(output().contains("Admin"));
        assertFalse(output().contains("Viewer"));
    }

    @Test
    void roleList_filterByPermission_returnsMatching() {
        var admin = roleWith("Admin");
        admin.addPermission(new Permission("READ", "Users", "чтение"));
        system.getRoleManager().add(admin);

        var viewer = roleWith("Viewer");
        system.getRoleManager().add(viewer);

        parser.executeCommand("role-list --permission READ Users", scannerWith(""));

        assertTrue(output().contains("Admin"));
        assertFalse(output().contains("Viewer"));
    }

    @Test
    void roleList_filterByMinPermissions_returnsMatching() {
        var admin = roleWith("Admin");
        admin.addPermission(new Permission("READ", "Users", "чтение"));
        admin.addPermission(new Permission("WRITE", "Users", "запись"));
        system.getRoleManager().add(admin);

        system.getRoleManager().add(roleWith("Viewer"));

        parser.executeCommand("role-list --min-permissions 2", scannerWith(""));

        assertTrue(output().contains("Admin"));
        assertFalse(output().contains("Viewer"));
    }

    // endregion

    // region role-view

    @Test
    void roleView_existingRole_printsInfo() {
        system.getRoleManager().add(roleWith("Admin"));
        parser.executeCommand("role-view Admin", scannerWith(""));
        assertTrue(output().contains("Admin"));
    }

    @Test
    void roleView_unknownRole_printsNotFound() {
        parser.executeCommand("role-view Nobody", scannerWith(""));
        assertTrue(output().contains("не найдена") || output().contains("не найден"));
    }

    // endregion

    // region role-update

    @Test
    void roleUpdate_newName_updatesSuccessfully() {
        system.getRoleManager().add(roleWith("Admin"));
        parser.executeCommand("role-update --name SuperAdmin Admin", scannerWith(""));
        assertTrue(system.getRoleManager().findByName("SuperAdmin").isPresent());
        assertTrue(system.getRoleManager().findByName("Admin").isEmpty());
    }

    @Test
    void roleUpdate_newDescription_updatesSuccessfully() {
        system.getRoleManager().add(roleWith("Admin"));
        parser.executeCommand("role-update --description NewDesc Admin", scannerWith(""));
        var role = system.getRoleManager().findByName("Admin").orElseThrow();
        assertEquals("NewDesc", role.getDescription());
    }

    @Test
    void roleUpdate_noFlags_printsError() {
        system.getRoleManager().add(roleWith("Admin"));
        parser.executeCommand("role-update Admin", scannerWith(""));
        assertTrue(output().contains("флагов"));
    }

    @Test
    void roleUpdate_unknownRole_printsNotFound() {
        parser.executeCommand("role-update --name X Nobody", scannerWith(""));
        assertTrue(output().contains("не найдена") || output().contains("не найден"));
    }

    // endregion

    // region role-delete

    @Test
    void roleDelete_confirmedYes_deletesRole() {
        system.getRoleManager().add(roleWith("Admin"));
        parser.executeCommand("role-delete Admin", scannerWith("y"));
        assertTrue(system.getRoleManager().findByName("Admin").isEmpty());
    }

    @Test
    void roleDelete_confirmedNo_keepsRole() {
        system.getRoleManager().add(roleWith("Admin"));
        parser.executeCommand("role-delete Admin", scannerWith("n"));
        assertTrue(system.getRoleManager().findByName("Admin").isPresent());
    }

    @Test
    void roleDelete_unknownRole_printsNotFound() {
        parser.executeCommand("role-delete Nobody", scannerWith("y"));
        assertTrue(output().contains("не найдена") || output().contains("не найден"));
    }

    // endregion

    // region role-add-permission

    @Test
    void roleAddPermission_addsPermissionToRole() {
        system.getRoleManager().add(roleWith("Admin"));
        parser.executeCommand("role-add-permission Admin READ Users чтение-пользователей", scannerWith(""));
        var role = system.getRoleManager().findByName("Admin").orElseThrow();
        assertTrue(role.hasPermission("READ", "Users"));
    }

    @Test
    void roleAddPermission_unknownRole_printsError() {
        parser.executeCommand("role-add-permission NoRole READ Users описание", scannerWith(""));
        assertTrue(output().contains("Ошибка"));
    }

    // endregion

    // region role-remove-permission

    @Test
    void roleRemovePermission_validIndex_removesPermission() {
        var role = roleWith("Admin");
        role.addPermission(new Permission("READ", "Users", "чтение"));
        system.getRoleManager().add(role);

        parser.executeCommand("role-remove-permission Admin", scannerWith("1"));

        var updated = system.getRoleManager().findByName("Admin").orElseThrow();
        assertFalse(updated.hasPermission("READ", "Users"));
    }

    @Test
    void roleRemovePermission_invalidIndex_printsError() {
        var role = roleWith("Admin");
        role.addPermission(new Permission("READ", "Users", "чтение"));
        system.getRoleManager().add(role);

        parser.executeCommand("role-remove-permission Admin", scannerWith("99"));

        assertTrue(output().contains("Неверный номер"));
    }

    @Test
    void roleRemovePermission_noPermissions_printsMessage() {
        system.getRoleManager().add(roleWith("Admin"));
        parser.executeCommand("role-remove-permission Admin", scannerWith("1"));
        assertTrue(output().contains("нет прав"));
    }

    @Test
    void roleRemovePermission_unknownRole_printsNotFound() {
        parser.executeCommand("role-remove-permission Nobody", scannerWith("1"));
        assertTrue(output().contains("не найдена") || output().contains("не найден"));
    }

    // endregion
}