package org.example.sorters;

import org.example.assignment.RoleAssignment;

import java.util.Comparator;

public class AssignmentSorters {

    Comparator<RoleAssignment> byUsername() {
        return Comparator.comparing(ra -> ra.user().username());
    }

    Comparator<RoleAssignment> byRoleName() {
        return Comparator.comparing(ra -> ra.role().getName());
    }

    Comparator<RoleAssignment> byAssignmentDate() {
        return Comparator.comparing(ra -> ra.metadata().assignedAt());
    }
}
