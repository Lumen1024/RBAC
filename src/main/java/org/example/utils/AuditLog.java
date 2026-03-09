package org.example.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AuditLog {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final List<AuditEntry> entries = new ArrayList<>();

    public void log(String action, String performer, String target, String details) {
        ValidationUtils.requireNonEmpty(action, "action");
        ValidationUtils.requireNonEmpty(performer, "performer");
        ValidationUtils.requireNonEmpty(target, "target");

        String timestamp = LocalDateTime.now().format(FORMATTER);
        entries.add(new AuditEntry(timestamp, action, performer, target, details));
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
        for (AuditEntry e : entries) {
            System.out.printf("[%s] %s | performer: %s | target: %s | details: %s%n",
                    e.timestamp(), e.action(), e.performer(), e.target(),
                    e.details() != null ? e.details() : "-");
        }
    }

    public void saveToFile(String filename) {
        ValidationUtils.requireNonEmpty(filename, "filename");
        try (PrintWriter writer = new PrintWriter(filename)) {
            for (AuditEntry e : entries) {
                writer.printf("[%s] %s | performer: %s | target: %s | details: %s%n",
                        e.timestamp(), e.action(), e.performer(), e.target(),
                        e.details() != null ? e.details() : "-");
            }
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