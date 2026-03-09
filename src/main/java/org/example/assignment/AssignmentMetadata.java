package org.example.assignment;

import org.example.utils.ValidationUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AssignmentMetadata(
        String assignedBy,
        String assignedAt,
        String reason
) {

    public AssignmentMetadata {
        ValidationUtils.requireNonEmpty(assignedAt, "assignedAt");
        ValidationUtils.requireNonEmpty(assignedBy, "assignedBy");
        if (ValidationUtils.isValidDate(assignedAt))
            throw new IllegalArgumentException("invalid time format");
        if (reason == null) {
            reason = "";
        }
    }

    public static AssignmentMetadata now(String assignedBy, String reason) {
        ValidationUtils.requireNonEmpty(assignedBy, "assignedBy");
        ValidationUtils.requireNonEmpty(reason, "reason");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return new AssignmentMetadata(assignedBy, timestamp, reason);
    }

    public String format() {
        String base = "Assigned by %s at %s".formatted(assignedBy, assignedAt);
        return reason.isEmpty() ? base : base + " (reason: " + reason + ")";
    }
}
