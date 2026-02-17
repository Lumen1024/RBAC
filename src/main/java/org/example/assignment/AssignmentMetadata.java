package org.example.assignment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AssignmentMetadata(
        String assignedBy,
        String assignedAt,
        String reason
) {

    public AssignmentMetadata {
        if (assignedBy == null || assignedBy.isBlank() || assignedAt == null || assignedAt.isBlank()) {
            throw new IllegalArgumentException("Все поля должны содержать значения");
        }

        if (reason == null) {
            reason = "";
        }
    }

    public static AssignmentMetadata now(String assignedBy, String reason) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return new AssignmentMetadata(assignedBy, timestamp, reason);
    }

    public String format() {
        String base = "Assigned by %s at %s".formatted(assignedBy, assignedAt);
        return reason.isEmpty() ? base : base + " (reason: " + reason + ")";
    }
}
