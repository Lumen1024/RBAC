package org.example.sorters;

import org.example.data.Role;

import java.util.Comparator;

public class RoleSorters {
    Comparator<Role> byName() {
        return Comparator.comparing(Role::getName);
    }

    Comparator<Role> byPermissionCount() {
        return Comparator.comparing(role -> role.getPermissions().size());
    }

}
