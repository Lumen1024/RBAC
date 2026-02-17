package org.example.filter;

import org.example.assignment.RoleAssignment;

@FunctionalInterface
public interface AssignmentFilter {

    boolean test(RoleAssignment assignment);

}
