package org.example;

import org.example.assignment.RoleAssignment;
import org.example.assignment.TemporaryAssignment;
import org.example.managers.AssignmentManager;
import org.example.managers.RoleManager;
import org.example.managers.UserManager;
import org.example.utils.AuditLog;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AssignmentCleanupService {

    private final AssignmentManager assignmentManager;
    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AuditLog auditLog;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = Thread.ofVirtual().unstarted(r);
                t.setName("assignment-cleanup");
                return t;
            });

    private ScheduledFuture<?> task;

    public AssignmentCleanupService(
            AssignmentManager assignmentManager,
            UserManager userManager,
            RoleManager roleManager,
            AuditLog auditLog
    ) {
        this.assignmentManager = assignmentManager;
        this.userManager = userManager;
        this.roleManager = roleManager;
        this.auditLog = auditLog;
    }

    public void start(int intervalSeconds) {
        task = scheduler.scheduleAtFixedRate(
                this::cleanup,
                intervalSeconds,
                intervalSeconds,
                TimeUnit.SECONDS
        );
    }

    public void stop() {
        if (task != null) task.cancel(false);
        scheduler.shutdown();
    }

    private void cleanup() {
        // --- фаза сканирования: читаем снимок без блокировок ---
        List<String> expiredIds = assignmentManager.findAll().stream()
                .filter(a -> a instanceof TemporaryAssignment ta && ta.isExpired())
                .map(RoleAssignment::assignmentId)
                .toList();

        // --- фаза отзыва: короткие критические секции по одному ---
        int revoked = 0;
        for (String id : expiredIds) {
            try {
                assignmentManager.revokeAssignment(id);
                revoked++;
            } catch (Exception ignored) {
                // назначение уже удалено параллельной операцией
            }
        }

        // --- статистика в лог ---
        int users = userManager.count();
        int roles = roleManager.count();
        int active = assignmentManager.getActiveAssignments().size();
        int total = assignmentManager.count();

        String details = "users=%d, roles=%d, active_assignments=%d, total_assignments=%d, expired_revoked=%d"
                .formatted(users, roles, active, total, revoked);

        auditLog.log("CLEANUP", "system", "assignments", details);
    }
}