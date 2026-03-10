package org.example.assignment;

import org.example.data.Role;
import org.example.data.User;
import org.example.utils.DateUtils;
import org.example.utils.ValidationUtils;

public class TemporaryAssignment extends AbstractRoleAssignment {

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
        if (ValidationUtils.isValidDate(expiresAt))
            throw new IllegalArgumentException("invalid time format");

        this.expiresAt = expiresAt;
        this.autoRenew = autoRenew;
    }

    public boolean isExpired() {
        return DateUtils.isAfter(DateUtils.getCurrentDate(), expiresAt.substring(0, 10));
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
        if (ValidationUtils.isValidDate(newExpirationDate))
            throw new IllegalArgumentException("invalid time format");
        this.expiresAt = newExpirationDate;
    }

    public String getTimeRemaining() {
        if (isExpired()) return "Expired";
        return DateUtils.formatRelativeTime(expiresAt.substring(0, 10));
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
