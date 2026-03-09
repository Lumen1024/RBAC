package org.example.sorters;

import org.example.data.User;

import java.util.Comparator;

public class UserSorters {
    Comparator<User> byUsername() {
        return Comparator.comparing(User::username);
    }

    Comparator<User> byFullName() {
        return Comparator.comparing(User::fullName);
    }

    Comparator<User> byEmail() {
        return Comparator.comparing(User::email);
    }
}
