package org.example.assignment;

import org.example.Role;
import org.example.User;

public interface RoleAssignment {
    String assignmentId();

    User user();

    Role role();

    AssignmentMetadata metadata();

    boolean isActive();

    String assignmentType();
}
