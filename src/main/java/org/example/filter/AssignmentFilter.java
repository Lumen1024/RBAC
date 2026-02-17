package org.example.filter;

import org.example.RoleAssignment;

@FunctionalInterface
public interface AssignmentFilter {

    boolean test(RoleAssignment assignment);

}
