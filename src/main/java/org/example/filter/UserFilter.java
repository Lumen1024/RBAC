package org.example.filter;

import org.example.User;

@FunctionalInterface
public interface UserFilter {

    boolean test(User user);
}

