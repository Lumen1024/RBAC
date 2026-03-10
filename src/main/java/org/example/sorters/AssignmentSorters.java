package org.example.sorters;

import org.example.assignment.RoleAssignment;

import java.util.Comparator;

public class AssignmentSorters {

    public static Comparator<RoleAssignment> byUsername() {
        return Comparator.comparing(ra -> ra.user().username());
    }

    public static Comparator<RoleAssignment> byRoleName() {
        return Comparator.comparing(ra -> ra.role().getName());
    }

    public static Comparator<RoleAssignment> byAssignmentDate() {
        return Comparator.comparing(ra -> ra.metadata().assignedAt());
    }
}
