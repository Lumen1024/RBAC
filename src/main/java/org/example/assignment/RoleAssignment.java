package org.example.assignment;

import org.example.data.Role;
import org.example.data.User;

public interface RoleAssignment {
    String assignmentId();

    User user();

    Role role();

    AssignmentMetadata metadata();

    boolean isActive();

    String assignmentType();
}
