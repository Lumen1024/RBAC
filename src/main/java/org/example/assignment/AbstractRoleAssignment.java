package org.example.assignment;

import org.example.data.Role;
import org.example.data.User;
import org.example.utils.ValidationUtils;

import java.util.Objects;
import java.util.UUID;

public abstract class AbstractRoleAssignment implements RoleAssignment {
    private final String assignmentId;
    private final User user;
    private final Role role;
    private final AssignmentMetadata metadata;

    protected AbstractRoleAssignment(User user, Role role, AssignmentMetadata metadata) {
        Objects.requireNonNull(user);
        Objects.requireNonNull(role);
        Objects.requireNonNull(metadata);

        this.assignmentId = "assign_" + UUID.randomUUID();
        this.user = user;
        this.role = role;
        this.metadata = metadata;
    }

    // region getters

    @Override
    public String assignmentId() {
        return assignmentId;
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public Role role() {
        return role;
    }

    @Override
    public AssignmentMetadata metadata() {
        return metadata;
    }

    // endregion

    public String summary() {
        return "[%s] %s assigned to %s by %s at %s\nReason: %s\nStatus: %s".formatted(
                assignmentType(),
                role.getName(),
                user.username(),
                metadata.assignedBy(),
                metadata.assignedAt(),
                metadata.reason().isEmpty() ? "N/A" : metadata.reason(),
                isActive() ? "ACTIVE" : "INACTIVE");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractRoleAssignment that)) return false;
        return assignmentId.equals(that.assignmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignmentId);
    }

    @Override
    public String toString() {
        return "Assignment{id='%s', type=%s, user=%s, role=%s}".formatted(
                assignmentId,
                assignmentType(),
                user.username(),
                role.getName()
        );
    }
}
