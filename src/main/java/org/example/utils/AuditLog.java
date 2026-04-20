package org.example.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class AuditLog {

    private final BlockingQueue<AuditEntry> queue = new LinkedBlockingQueue<>();
    private final List<AuditEntry> entries = new CopyOnWriteArrayList<>();

    public AuditLog() {
        Thread.ofVirtual().start(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    entries.add(queue.take());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public void log(String action, String performer, String target, String details) {
        ValidationUtils.requireNonEmpty(action, "action");
        ValidationUtils.requireNonEmpty(performer, "performer");
        ValidationUtils.requireNonEmpty(target, "target");

        queue.add(new AuditEntry(DateUtils.getCurrentDateTime(), action, performer, target, details));
    }

    public List<AuditEntry> getAll() {
        return List.copyOf(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        ValidationUtils.requireNonEmpty(performer, "performer");
        return entries.stream()
                .filter(e -> e.performer().equals(performer))
                .toList();
    }

    public List<AuditEntry> getByAction(String action) {
        ValidationUtils.requireNonEmpty(action, "action");
        return entries.stream()
                .filter(e -> e.action().equals(action))
                .toList();
    }

    public void printLog() {
        if (entries.isEmpty()) {
            System.out.println("Логи пусты");
            return;
        }
        var headers = new String[]{"TIMESTAMP", "ACTION", "PERFORMER", "TARGET", "DETAILS"};
        var rows = entries.stream()
                .map(e -> new String[]{
                        e.timestamp(), e.action(), e.performer(), e.target(),
                        e.details() != null ? e.details() : "-"
                })
                .toList();
        System.out.println(FormatUtils.formatTable("Audit Log", headers, rows));
    }

    public void saveToFile(String filename) {
        ValidationUtils.requireNonEmpty(filename, "filename");
        var headers = new String[]{"TIMESTAMP", "ACTION", "PERFORMER", "TARGET", "DETAILS"};
        var rows = entries.stream()
                .map(e -> new String[]{
                        e.timestamp(), e.action(), e.performer(), e.target(),
                        e.details() != null ? e.details() : "-"
                })
                .toList();
        try (PrintWriter writer = new PrintWriter(filename)) {
            writer.println(FormatUtils.formatTable("Audit Log", headers, rows));
        } catch (IOException ex) {
            throw new RuntimeException("Ошибка записи в файл: " + filename, ex);
        }
    }

    public record AuditEntry(
            String timestamp,
            String action,
            String performer,
            String target,
            String details
    ) {
    }
}