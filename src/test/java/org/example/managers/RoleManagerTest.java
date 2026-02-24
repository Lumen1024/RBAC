package org.example.managers;

import org.example.Permission;
import org.example.Role;
import org.example.filter.RoleFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RoleManagerTest {

    private RoleManager manager;

    @BeforeEach
    void setUp() {
        manager = new RoleManager();
    }

    private Role role(String name) {
        return new Role(name, "Description of " + name);
    }

    private Permission perm(String name, String resource) {
        return new Permission(name, resource, "Permission " + name + " on " + resource);
    }

    // region add

    @Test
    void add_addsRoleSuccessfully() {
        manager.add(role("ADMIN"));
        assertEquals(1, manager.count());
    }

    @Test
    void add_throwsNPE_whenNull() {
        assertThrows(NullPointerException.class, () -> manager.add(null));
    }

    @Test
    void add_throwsIllegalState_whenDuplicateName() {
        manager.add(role("ADMIN"));
        assertThrows(IllegalStateException.class, () -> manager.add(role("ADMIN")));
    }

    // endregion

    // region remove

    @Test
    void remove_returnsTrue_whenRoleExists() {
        Role r = role("ADMIN");
        manager.add(r);
        assertTrue(manager.remove(r));
        assertEquals(0, manager.count());
    }

    @Test
    void remove_returnsFalse_whenNull() {
        assertFalse(manager.remove(null));
    }

    @Test
    void remove_returnsFalse_whenRoleNotRegistered() {
        assertFalse(manager.remove(role("ADMIN")));
    }

    @Test
    void remove_throwsIllegalState_whenRoleIsAssigned() {
        Role r = role("ADMIN");
        manager.add(r);
        manager.setAssignmentCheck(role -> role.equals(r));

        assertThrows(IllegalStateException.class, () -> manager.remove(r));
    }

    @Test
    void remove_succeedsWhenAssignmentCheckReturnsFalse() {
        Role r = role("ADMIN");
        manager.add(r);
        manager.setAssignmentCheck(_ -> false);

        assertTrue(manager.remove(r));
    }

    // endregion


    // region findById

    @Test
    void findById_returnsRole_whenExists() {
        Role r = role("ADMIN");
        manager.add(r);
        Optional<Role> result = manager.findById(r.getId());
        assertTrue(result.isPresent());
        assertEquals(r, result.get());
    }

    @Test
    void findById_returnsEmpty_whenNotFound() {
        assertTrue(manager.findById("nonexistent-id").isEmpty());
    }

    // endregion

    // region findAll

    @Test
    void findAll_returnsAllRoles() {
        manager.add(role("ADMIN"));
        manager.add(role("USER"));
        assertEquals(2, manager.findAll().size());
    }

    @Test
    void findAll_returnsEmptyList_initially() {
        assertTrue(manager.findAll().isEmpty());
    }

    @Test
    void findAll_withFilterAndSorter_returnsFilteredSortedList() {
        manager.add(role("USER"));
        manager.add(role("ADMIN"));
        manager.add(role("MODERATOR"));

        List<Role> result = manager.findAll(_ -> true, Comparator.comparing(Role::getName));

        assertEquals(3, result.size());
        assertEquals("ADMIN", result.get(0).getName());
        assertEquals("MODERATOR", result.get(1).getName());
        assertEquals("USER", result.get(2).getName());
    }

    @Test
    void findAll_throwsNPE_whenFilterNull() {
        assertThrows(NullPointerException.class,
                () -> manager.findAll(null, Comparator.comparing(Role::getName)));
    }

    @Test
    void findAll_throwsNPE_whenSorterNull() {
        assertThrows(NullPointerException.class,
                () -> manager.findAll(_ -> true, null));
    }

    // endregion

    // region findByName

    @Test
    void findByName_returnsRole_whenExists() {
        manager.add(role("ADMIN"));
        assertTrue(manager.findByName("ADMIN").isPresent());
    }

    @Test
    void findByName_returnsEmpty_whenNotFound() {
        assertTrue(manager.findByName("NONEXISTENT").isEmpty());
    }

    // endregion

    // region findByFilter

    @Test
    void findByFilter_returnsMatchingRoles() {
        manager.add(role("ADMIN"));
        manager.add(role("USER"));

        RoleFilter filter = r -> r.getName().startsWith("A");
        List<Role> result = manager.findByFilter(filter);

        assertEquals(1, result.size());
        assertEquals("ADMIN", result.getFirst().getName());
    }

    @Test
    void findByFilter_returnsEmpty_whenNoMatch() {
        manager.add(role("ADMIN"));
        List<Role> result = manager.findByFilter(r -> r.getName().startsWith("Z"));
        assertTrue(result.isEmpty());
    }

    @Test
    void findByFilter_throwsNPE_whenNull() {
        assertThrows(NullPointerException.class, () -> manager.findByFilter(null));
    }

    // endregion


    // region addPermissionToRole

    @Test
    void addPermissionToRole_addsPermissionSuccessfully() {
        manager.add(role("ADMIN"));
        Permission p = perm("READ", "users");
        manager.addPermissionToRole("ADMIN", p);

        Role r = manager.findByName("ADMIN").orElseThrow();
        assertTrue(r.hasPermission(p));
    }

    @Test
    void addPermissionToRole_throwsNoSuchElement_whenRoleNotFound() {
        assertThrows(NoSuchElementException.class,
                () -> manager.addPermissionToRole("NONEXISTENT", perm("READ", "users")));
    }

    @Test
    void addPermissionToRole_throwsNPE_whenPermissionNull() {
        manager.add(role("ADMIN"));
        assertThrows(NullPointerException.class,
                () -> manager.addPermissionToRole("ADMIN", null));
    }

    // endregion

    // region removePermissionFromRole

    @Test
    void removePermissionFromRole_removesPermission() {
        manager.add(role("ADMIN"));
        Permission p = perm("READ", "users");
        manager.addPermissionToRole("ADMIN", p);
        manager.removePermissionFromRole("ADMIN", p);

        Role r = manager.findByName("ADMIN").orElseThrow();
        assertFalse(r.hasPermission(p));
    }

    @Test
    void removePermissionFromRole_throwsNoSuchElement_whenRoleNotFound() {
        assertThrows(NoSuchElementException.class,
                () -> manager.removePermissionFromRole("NONEXISTENT", perm("READ", "users")));
    }

    @Test
    void removePermissionFromRole_throwsNPE_whenPermissionNull() {
        manager.add(role("ADMIN"));
        assertThrows(NullPointerException.class,
                () -> manager.removePermissionFromRole("ADMIN", null));
    }

    // endregion

    // region findRolesWithPermission

    @Test
    void findRolesWithPermission_returnsRolesWithMatchingPermission() {
        Role admin = role("ADMIN");
        Role user = role("USER");
        manager.add(admin);
        manager.add(user);

        manager.addPermissionToRole("ADMIN", perm("READ", "documents"));
        manager.addPermissionToRole("USER", perm("WRITE", "documents"));

        List<Role> result = manager.findRolesWithPermission("READ", "documents");
        assertEquals(1, result.size());
        assertEquals("ADMIN", result.getFirst().getName());
    }

    @Test
    void findRolesWithPermission_returnsEmpty_whenNoMatch() {
        manager.add(role("ADMIN"));
        List<Role> result = manager.findRolesWithPermission("DELETE", "users");
        assertTrue(result.isEmpty());
    }

    @Test
    void findRolesWithPermission_throwsNPE_whenPermissionNameNull() {
        assertThrows(NullPointerException.class,
                () -> manager.findRolesWithPermission(null, "users"));
    }

    @Test
    void findRolesWithPermission_throwsNPE_whenResourceNull() {
        assertThrows(NullPointerException.class,
                () -> manager.findRolesWithPermission("READ", null));
    }

    // endregion


    // region exists

    @Test
    void exists_returnsTrue_whenRoleAdded() {
        manager.add(role("ADMIN"));
        assertTrue(manager.exists("ADMIN"));
    }

    @Test
    void exists_returnsFalse_whenRoleAbsent() {
        assertFalse(manager.exists("NONEXISTENT"));
    }

    // endregion

    // region count

    @Test
    void count_returnsZero_initially() {
        assertEquals(0, manager.count());
    }

    @Test
    void count_incrementsAfterAdd() {
        manager.add(role("ADMIN"));
        assertEquals(1, manager.count());
    }

    // endregion

    // region clear

    @Test
    void clear_removesAllRoles() {
        manager.add(role("ADMIN"));
        manager.add(role("USER"));
        manager.clear();
        assertEquals(0, manager.count());
    }

    // endregion

    // region equals, hashCode, toString

    @Test
    void equals_returnsTrue_whenSameContent() {
        Role r = role("ADMIN");
        RoleManager other = new RoleManager();
        manager.add(r);
        other.add(r);
        assertEquals(manager, other);
    }

    @Test
    void equals_returnsFalse_whenDifferentContent() {
        RoleManager other = new RoleManager();
        manager.add(role("ADMIN"));
        other.add(role("USER"));
        assertNotEquals(manager, other);
    }

    @Test
    void hashCode_equalsForSameContent() {
        Role r = role("ADMIN");
        RoleManager other = new RoleManager();
        manager.add(r);
        other.add(r);
        assertEquals(manager.hashCode(), other.hashCode());
    }

    @Test
    void toString_containsCount() {
        manager.add(role("ADMIN"));
        assertTrue(manager.toString().contains("1"));
    }

    // endregion
}
