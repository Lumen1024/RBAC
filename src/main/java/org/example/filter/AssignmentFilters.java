package org.example.filter;

import org.example.data.Role;
import org.example.data.User;
import org.example.assignment.RoleAssignment;
import org.example.assignment.TemporaryAssignment;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class AssignmentFilters {
    AssignmentFilter byUser(User user) {
        return a -> a.user() == user;
    }

    AssignmentFilter byUsername(String username) {
        return a -> a.user().username().equals(username);
    }

    AssignmentFilter byRole(Role role) {
        return a -> a.role() == role;
    }

    AssignmentFilter byRoleName(String roleName) {
        return a -> a.role().getName().equals(roleName);
    }

    AssignmentFilter activeOnly() {
        return RoleAssignment::isActive;
    }

    AssignmentFilter inactiveOnly() {
        return a -> !a.isActive();
    }

    AssignmentFilter byType(String type) {
        return a -> a.assignmentType().equals(type);
    }

    AssignmentFilter assignedBy(String username) {
        return a -> a.metadata().assignedBy().equals(username);
    }

    AssignmentFilter assignedAfter(String date) {
        return a -> ChronoUnit.MILLIS.between(
                LocalDateTime.parse(a.metadata().assignedAt()),
                LocalDateTime.parse(date)
        ) > 0;
    }

    AssignmentFilter expiringBefore(String date) {
        return a -> a instanceof TemporaryAssignment && ChronoUnit.MILLIS.between(
                LocalDateTime.parse(((TemporaryAssignment) a).getExpiresAt()),
                LocalDateTime.parse(date)
        ) > 0;
    }
}
