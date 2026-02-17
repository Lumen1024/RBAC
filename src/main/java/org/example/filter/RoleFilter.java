package org.example.filter;

import org.example.Role;

@FunctionalInterface
public interface RoleFilter {

    boolean test(Role role);

}
