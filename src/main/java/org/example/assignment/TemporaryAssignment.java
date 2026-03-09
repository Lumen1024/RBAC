package org.example.assignment;

import org.example.data.Role;
import org.example.data.User;
import org.example.utils.ValidationUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class TemporaryAssignment extends AbstractRoleAssignment {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private String expiresAt;
    private final boolean autoRenew;

    public TemporaryAssignment(
            User user,
            Role role,
            AssignmentMetadata metadata,
            String expiresAt,
            boolean autoRenew
    ) {
        super(user, role, metadata);
        ValidationUtils.requireNonEmpty(expiresAt, "expiresAt");

        this.expiresAt = expiresAt;
        this.autoRenew = autoRenew;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(LocalDateTime.parse(expiresAt, FORMATTER));
    }

    @Override
    public boolean isActive() {
        return !isExpired();
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public void extend(String newExpirationDate) {
        ValidationUtils.requireNonEmpty(newExpirationDate, "newExpirationDate");
        this.expiresAt = newExpirationDate;
    }

    public String getTimeRemaining() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expires = LocalDateTime.parse(expiresAt, FORMATTER);
        if (now.isAfter(expires)) {
            return "Expired";
        }
        long days = ChronoUnit.DAYS.between(now, expires);
        long hours = ChronoUnit.HOURS.between(now, expires) % 24;
        return "%d days, %d hours remaining".formatted(days, hours);
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    @Override
    public String summary() {
        return super.summary() + "\nExpires at: %s\nAuto-renew: %s\nTime remaining: %s".formatted(
                expiresAt,
                autoRenew ? "YES" : "NO",
                getTimeRemaining()
        );
    }
}
