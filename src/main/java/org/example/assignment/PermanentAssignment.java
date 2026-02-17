package org.example.assignment;

import org.example.Role;
import org.example.User;

public class PermanentAssignment extends AbstractRoleAssignment {
    private boolean revoked = false;

    public PermanentAssignment(User user, Role role, AssignmentMetadata metadata) {
        super(user, role, metadata);
    }

    public void revoke() {
        this.revoked = true;
    }

    @Override
    public boolean isActive() {
        return !revoked;
    }

    @Override
    public String assignmentType() {
        return "PERMANENT";
    }
}
