package org.example;

import org.example.assignment.AssignmentMetadata;
import org.example.assignment.PermanentAssignment;
import org.example.data.Permission;
import org.example.data.Role;
import org.example.data.User;
import org.example.filter.UserFilters;
import org.example.managers.AssignmentManager;
import org.example.managers.RoleManager;
import org.example.managers.UserManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class StressTest {

    private static final int THREADS = 10;
    private static final int OPS_PER_THREAD = 20;

    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;

    private static final List<String> ROLE_NAMES = List.of(
            "Admin", "Editor", "Viewer", "Moderator", "Support"
    );

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager(userManager, roleManager);

        for (String name : ROLE_NAMES) {
            Role role = new Role(name, name + " role");
            role.addPermission(new Permission("READ", name.toLowerCase(), "stress"));
            role.addPermission(new Permission("WRITE", name.toLowerCase(), "stress"));
            roleManager.add(role);
        }
    }

    /**
     * Множество потоков одновременно создают пользователей, назначают роли и
     * выполняют поиск. Проверяем отсутствие дубликатов и гонок.
     */
    @Test
    void concurrentCreateAndAssign() throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        for (int t = 0; t < THREADS; t++) {
            int tid = t;
            Thread.ofVirtual().start(() -> {
                try {
                    start.await();
                    for (int i = 0; i < OPS_PER_THREAD; i++) {
                        String username = "usr_t%d_i%d".formatted(tid, i);

                        // создание пользователя
                        try {
                            userManager.add(User.create(
                                    username,
                                    "User %d_%d".formatted(tid, i),
                                    "u%d_%d@stress.com".formatted(tid, i)
                            ));
                        } catch (IllegalStateException ignored) {
                            // уже существует — нормально при повторных вызовах
                        }

                        // назначение роли
                        String roleName = ROLE_NAMES.get(i % ROLE_NAMES.size());
                        userManager.findByUsername(username).ifPresent(user -> {
                            roleManager.findByName(roleName).ifPresent(role -> {
                                try {
                                    assignmentManager.add(new PermanentAssignment(
                                            user, role,
                                            AssignmentMetadata.now("stress-test", "load")
                                    ));
                                } catch (IllegalStateException ignored) {
                                    // дубликат назначения — нормально
                                }
                            });
                        });

                        // поиск и фильтрация
                        userManager.findByFilter(UserFilters.byUsernameContains("usr_t" + tid));
                        userManager.findAll();
                        assignmentManager.getActiveAssignments();
                        assignmentManager.findAll();
                    }
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "Потоки не завершились вовремя");

        assertTrue(errors.isEmpty(),
                "Необработанные исключения в потоках:\n" +
                errors.stream().map(Throwable::toString).collect(Collectors.joining("\n")));

        // нет дубликатов пользователей
        List<User> allUsers = userManager.findAll();
        long uniqueUsernames = allUsers.stream().map(User::username).distinct().count();
        assertEquals(allUsers.size(), uniqueUsernames, "Обнаружены дубликаты пользователей");

        // нет дубликатов активных назначений по паре user+role
        List<org.example.assignment.RoleAssignment> active = assignmentManager.getActiveAssignments();
        long uniquePairs = active.stream()
                .map(a -> a.user().username() + ":" + a.role().getName())
                .distinct()
                .count();
        assertEquals(active.size(), uniquePairs, "Обнаружены дубликаты активных назначений");

        // count() согласован с findAll()
        assertEquals(allUsers.size(), userManager.count());
        assertEquals(roleManager.findAll().size(), roleManager.count());
    }

    /**
     * Конкурентное обновление одного пользователя: объект должен всегда
     * оставаться в согласованном состоянии, без null-полей.
     */
    @Test
    void concurrentUpdateSameUser() throws InterruptedException {
        userManager.add(User.create("shared", "Initial Name", "initial@stress.com"));

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        for (int t = 0; t < THREADS; t++) {
            int tid = t;
            Thread.ofVirtual().start(() -> {
                try {
                    start.await();
                    userManager.update("shared", "Name" + tid, "upd%d@stress.com".formatted(tid));
                    User u = userManager.findByUsername("shared").orElseThrow();
                    assertNotNull(u.fullName(), "fullName не должен быть null");
                    assertNotNull(u.email(), "email не должен быть null");
                    assertFalse(u.fullName().isEmpty());
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "Потоки не завершились вовремя");

        assertTrue(errors.isEmpty(),
                "Ошибки при конкурентном обновлении:\n" +
                errors.stream().map(Throwable::toString).collect(Collectors.joining("\n")));

        // ровно один пользователь, без клонов
        assertEquals(1, userManager.count());
        assertNotNull(userManager.findByUsername("shared").orElseThrow().fullName());
    }

    /**
     * Конкурентное создание ролей с одинаковым именем: только одна должна
     * попасть в менеджер, остальные — получить IllegalStateException.
     */
    @Test
    void concurrentDuplicateRoleCreation() throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        List<Boolean> successes = new CopyOnWriteArrayList<>();

        for (int t = 0; t < THREADS; t++) {
            Thread.ofVirtual().start(() -> {
                try {
                    start.await();
                    roleManager.add(new Role("DuplicateRole", "desc"));
                    successes.add(true);
                } catch (IllegalStateException e) {
                    successes.add(false);
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "Потоки не завершились вовремя");

        assertTrue(errors.isEmpty(), "Неожиданные исключения: " + errors);

        // ровно одна успешная вставка
        long created = successes.stream().filter(b -> b).count();
        assertEquals(1, created, "Роль была добавлена %d раз вместо 1".formatted(created));

        // в хранилище ровно одна роль с таким именем
        long inStore = roleManager.findAll().stream()
                .filter(r -> r.getName().equals("DuplicateRole"))
                .count();
        assertEquals(1, inStore);
    }
}