package org.example;

import org.example.command.CommandParser;
import org.example.command.CommandRegistry;
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

class IntegrationTest {

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
        CommandRegistry.registerRoleCommands(parser);
        CommandRegistry.registerAssignmentCommands(parser);
        CommandRegistry.registerPermissionCommands(parser);

        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
    }

    private String output() { return out.toString(); }
    private Scanner scanner(String input) {
        return new Scanner(new ByteArrayInputStream(input.getBytes()));
    }
    private void cmd(String command) { parser.executeCommand(command, scanner("")); }
    private void cmd(String command, String input) { parser.executeCommand(command, scanner(input)); }

    /**
     * Полный жизненный цикл: создаём роли с правами → создаём пользователей →
     * назначаем роли → проверяем права → отзываем → удаляем.
     */
    @Test
    void fullLifecycle() {
        // 1. Создаём роли и добавляем им права
        cmd("role-create Admin администратор");
        cmd("role-add-permission Admin READ users чтение");
        cmd("role-add-permission Admin WRITE users запись");
        cmd("role-add-permission Admin DELETE users удаление");

        cmd("role-create Viewer наблюдатель");
        cmd("role-add-permission Viewer READ users чтение");

        // 2. Создаём пользователей
        cmd("user-create alice AliceFoo alice@corp.com");
        cmd("user-create bob BobBar bob@corp.com");

        // 3. Назначаем роли
        cmd("assign-role alice Admin по-необходимости");
        cmd("assign-role bob Viewer по-необходимости");

        // 4. Проверяем что alice имеет права Admin
        assertTrue(system.getAssignmentManager().userHasPermission(
                system.getUserManager().findByUsername("alice").orElseThrow(), "WRITE", "users"));
        assertFalse(system.getAssignmentManager().userHasPermission(
                system.getUserManager().findByUsername("bob").orElseThrow(), "WRITE", "users"));

        // 5. permissions-check через команду
        cmd("permissions-check alice WRITE users");
        assertTrue(output().contains("ИМЕЕТ"));

        cmd("permissions-check bob DELETE users");
        assertTrue(output().contains("НЕ ИМЕЕТ"));

        // 6. Обновляем alice, понижаем до Viewer
        cmd("revoke-role alice Admin");
        cmd("assign-role alice Viewer по-запросу");

        assertFalse(system.getAssignmentManager().userHasPermission(
                system.getUserManager().findByUsername("alice").orElseThrow(), "DELETE", "users"));
        assertTrue(system.getAssignmentManager().userHasPermission(
                system.getUserManager().findByUsername("alice").orElseThrow(), "READ", "users"));

        // 7. Удаляем bob — его назначения тоже должны исчезнуть
        var bob = system.getUserManager().findByUsername("bob").orElseThrow();
        cmd("user-delete bob", "yes");

        assertTrue(system.getUserManager().findByUsername("bob").isEmpty());
        assertTrue(system.getAssignmentManager().findByUser(bob).isEmpty());

        // 8. Итоговое состояние: только alice с ролью Viewer
        assertEquals(1, system.getUserManager().count());
        assertEquals(1, system.getAssignmentManager().findAll().stream().filter(a -> a.isActive()).count());
    }

    /**
     * Удаление роли каскадно снимает её со всех пользователей.
     */
    @Test
    void deleteRole_cascadesAssignments() {
        cmd("role-create Temp временная");
        cmd("user-create carol CarolC carol@corp.com");
        cmd("user-create dan DanD dan@corp.com");
        cmd("assign-role carol Temp причина");
        cmd("assign-role dan Temp причина");

        var role = system.getRoleManager().findByName("Temp").orElseThrow();
        assertEquals(2, system.getAssignmentManager().findByRole(role).size());

        cmd("role-delete Temp", "yes");

        assertTrue(system.getRoleManager().findByName("Temp").isEmpty());
        assertTrue(system.getAssignmentManager().findAll().isEmpty());
    }
}