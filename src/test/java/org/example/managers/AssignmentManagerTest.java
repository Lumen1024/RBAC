package org.example.managers;

import org.example.Permission;
import org.example.Role;
import org.example.User;
import org.example.assignment.AssignmentMetadata;
import org.example.assignment.PermanentAssignment;
import org.example.assignment.RoleAssignment;
import org.example.assignment.TemporaryAssignment;
import org.example.filter.AssignmentFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AssignmentManagerTest {

    // region setup

    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager manager;

    private User alice;
    private User bob;
    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        roleManager = new RoleManager();
        manager = new AssignmentManager(userManager, roleManager);

        alice = User.create("Alice", "Alice Smith", "alice@example.com");
        bob = User.create("bob_99", "Bob Jones", "bob@example.com");
        adminRole = new Role("ADMIN", "Administrator role");
        userRole = new Role("USER", "Regular user role");

        userManager.add(alice);
        userManager.add(bob);
        roleManager.add(adminRole);
        roleManager.add(userRole);
    }

    private AssignmentMetadata meta() {
        return AssignmentMetadata.now("system", "test");
    }

    private PermanentAssignment permanent(User user, Role role) {
        return new PermanentAssignment(user, role, meta());
    }

    private TemporaryAssignment temporary(User user, Role role, boolean future) {
        String date = LocalDateTime.now()
                .plusDays(future ? 1 : -1)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return new TemporaryAssignment(user, role, meta(), date, false);
    }

    // endregion


    // region constructor

    @Test
    void constructor_throwsNPE_whenUserManagerNull() {
        assertThrows(NullPointerException.class,
                () -> new AssignmentManager(null, roleManager));
    }

    @Test
    void constructor_throwsNPE_whenRoleManagerNull() {
        assertThrows(NullPointerException.class,
                () -> new AssignmentManager(userManager, null));
    }

    // endregion

    // region add

    @Test
    void add_addsAssignmentSuccessfully() {
        manager.add(permanent(alice, adminRole));
        assertEquals(1, manager.count());
    }

    @Test
    void add_throwsNPE_whenNull() {
        assertThrows(NullPointerException.class, () -> manager.add(null));
    }

    @Test
    void add_throwsNoSuchElement_whenUserNotInManager() {
        User stranger = User.create("stranger", "Stranger", "stranger@example.com");
        assertThrows(NoSuchElementException.class,
                () -> manager.add(permanent(stranger, adminRole)));
    }

    @Test
    void add_throwsNoSuchElement_whenRoleNotInManager() {
        Role unknownRole = new Role("UNKNOWN", "Not registered");
        assertThrows(NoSuchElementException.class,
                () -> manager.add(permanent(alice, unknownRole)));
    }

    @Test
    void add_throwsIllegalState_whenDuplicateActiveAssignment() {
        manager.add(permanent(alice, adminRole));
        assertThrows(IllegalStateException.class,
                () -> manager.add(permanent(alice, adminRole)));
    }

    @Test
    void add_allowsDuplicateRoleAfterInactiveAssignment() {
        TemporaryAssignment expired = temporary(alice, adminRole, false);
        manager.add(expired);
        assertDoesNotThrow(() -> manager.add(permanent(alice, adminRole)));
    }

    // endregion

    // region remove

    @Test
    void remove_returnsTrue_whenAssignmentExists() {
        PermanentAssignment a = permanent(alice, adminRole);
        manager.add(a);
        assertTrue(manager.remove(a));
        assertEquals(0, manager.count());
    }

    @Test
    void remove_returnsFalse_whenNull() {
        assertFalse(manager.remove(null));
    }

    @Test
    void remove_returnsFalse_whenAssignmentNotRegistered() {
        assertFalse(manager.remove(permanent(alice, adminRole)));
    }

    // endregion


    // region findById

    @Test
    void findById_returnsAssignment_whenExists() {
        PermanentAssignment a = permanent(alice, adminRole);
        manager.add(a);
        assertTrue(manager.findById(a.assignmentId()).isPresent());
    }

    @Test
    void findById_returnsEmpty_whenNotFound() {
        assertTrue(manager.findById("nonexistent-id").isEmpty());
    }

    // endregion

    // region findAll

    @Test
    void findAll_returnsAllAssignments() {
        manager.add(permanent(alice, adminRole));
        manager.add(permanent(bob, userRole));
        assertEquals(2, manager.findAll().size());
    }

    @Test
    void findAll_returnsEmptyList_initially() {
        assertTrue(manager.findAll().isEmpty());
    }

    @Test
    void findAll_withFilterAndSorter_returnsFilteredSortedList() {
        manager.add(permanent(bob, adminRole));
        manager.add(permanent(alice, adminRole));

        List<RoleAssignment> result = manager.findAll(
                a -> a.role().equals(adminRole),
                Comparator.comparing(a -> a.user().username())
        );

        assertEquals(2, result.size());
        assertEquals(alice, result.get(0).user());
        assertEquals(bob, result.get(1).user());
    }

    @Test
    void findAll_throwsNPE_whenFilterNull() {
        assertThrows(NullPointerException.class,
                () -> manager.findAll(null, Comparator.comparing(RoleAssignment::assignmentId)));
    }

    @Test
    void findAll_throwsNPE_whenSorterNull() {
        assertThrows(NullPointerException.class,
                () -> manager.findAll(_ -> true, null));
    }

    // endregion

    // region findByUser

    @Test
    void findByUser_returnsAssignmentsForUser() {
        manager.add(permanent(alice, adminRole));
        manager.add(permanent(alice, userRole));
        manager.add(permanent(bob, userRole));

        List<RoleAssignment> result = manager.findByUser(alice);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(a -> a.user().equals(alice)));
    }

    @Test
    void findByUser_returnsEmpty_whenUserHasNoAssignments() {
        assertTrue(manager.findByUser(alice).isEmpty());
    }

    @Test
    void findByUser_throwsNPE_whenNull() {
        assertThrows(NullPointerException.class, () -> manager.findByUser(null));
    }

    // endregion

    // region findByRole

    @Test
    void findByRole_returnsAssignmentsForRole() {
        manager.add(permanent(alice, adminRole));
        manager.add(permanent(bob, adminRole));
        manager.add(permanent(alice, userRole));

        List<RoleAssignment> result = manager.findByRole(adminRole);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(a -> a.role().equals(adminRole)));
    }

    @Test
    void findByRole_returnsEmpty_whenRoleNotAssigned() {
        assertTrue(manager.findByRole(adminRole).isEmpty());
    }

    @Test
    void findByRole_throwsNPE_whenNull() {
        assertThrows(NullPointerException.class, () -> manager.findByRole(null));
    }

    // endregion

    // region findByFilter

    @Test
    void findByFilter_returnsMatchingAssignments() {
        manager.add(permanent(alice, adminRole));
        manager.add(permanent(bob, userRole));

        AssignmentFilter filter = a -> a.user().equals(alice);
        List<RoleAssignment> result = manager.findByFilter(filter);

        assertEquals(1, result.size());
        assertEquals(alice, result.getFirst().user());
    }

    @Test
    void findByFilter_throwsNPE_whenNull() {
        assertThrows(NullPointerException.class, () -> manager.findByFilter(null));
    }

    // endregion


    // region getActiveAssignments

    @Test
    void getActiveAssignments_returnsOnlyActive() {
        manager.add(permanent(alice, adminRole));
        manager.add(temporary(bob, userRole, false)); // expired

        List<RoleAssignment> active = manager.getActiveAssignments();
        assertEquals(1, active.size());
        assertEquals(alice, active.getFirst().user());
    }

    @Test
    void getActiveAssignments_includesActiveTemporary() {
        manager.add(temporary(alice, adminRole, true)); // future
        assertEquals(1, manager.getActiveAssignments().size());
    }

    // endregion

    // region getExpiredAssignments

    @Test
    void getExpiredAssignments_returnsOnlyInactive() {
        manager.add(permanent(alice, adminRole));
        manager.add(temporary(bob, userRole, false)); // expired

        List<RoleAssignment> expired = manager.getExpiredAssignments();
        assertEquals(1, expired.size());
        assertEquals(bob, expired.getFirst().user());
    }

    @Test
    void getExpiredAssignments_returnsEmpty_whenAllActive() {
        manager.add(permanent(alice, adminRole));
        assertTrue(manager.getExpiredAssignments().isEmpty());
    }

    // endregion


    // region userHasRole

    @Test
    void userHasRole_returnsTrue_whenActiveAssignmentExists() {
        manager.add(permanent(alice, adminRole));
        assertTrue(manager.userHasRole(alice, adminRole));
    }

    @Test
    void userHasRole_returnsFalse_whenNoAssignment() {
        assertFalse(manager.userHasRole(alice, adminRole));
    }

    @Test
    void userHasRole_returnsFalse_whenAssignmentIsInactive() {
        manager.add(temporary(alice, adminRole, false)); // expired
        assertFalse(manager.userHasRole(alice, adminRole));
    }

    @Test
    void userHasRole_throwsNPE_whenUserNull() {
        assertThrows(NullPointerException.class,
                () -> manager.userHasRole(null, adminRole));
    }

    @Test
    void userHasRole_throwsNPE_whenRoleNull() {
        assertThrows(NullPointerException.class,
                () -> manager.userHasRole(alice, null));
    }

    // endregion

    // region userHasPermission

    @Test
    void userHasPermission_returnsTrue_whenUserHasPermissionThroughRole() {
        adminRole.addPermission(new Permission("READ", "documents", "Read documents"));
        manager.add(permanent(alice, adminRole));

        assertTrue(manager.userHasPermission(alice, "READ", "documents"));
    }

    @Test
    void userHasPermission_returnsFalse_whenUserHasNoSuchPermission() {
        manager.add(permanent(alice, userRole));
        assertFalse(manager.userHasPermission(alice, "DELETE", "documents"));
    }

    @Test
    void userHasPermission_returnsFalse_whenAssignmentIsInactive() {
        adminRole.addPermission(new Permission("READ", "documents", "Read documents"));
        manager.add(temporary(alice, adminRole, false)); // expired

        assertFalse(manager.userHasPermission(alice, "READ", "documents"));
    }

    @Test
    void userHasPermission_throwsNPE_whenUserNull() {
        assertThrows(NullPointerException.class,
                () -> manager.userHasPermission(null, "READ", "documents"));
    }

    @Test
    void userHasPermission_throwsNPE_whenPermissionNameNull() {
        assertThrows(NullPointerException.class,
                () -> manager.userHasPermission(alice, null, "documents"));
    }

    @Test
    void userHasPermission_throwsNPE_whenResourceNull() {
        assertThrows(NullPointerException.class,
                () -> manager.userHasPermission(alice, "READ", null));
    }

    // endregion

    // region getUserPermissions

    @Test
    void getUserPermissions_returnsPermissionsFromActiveRoles() {
        Permission readDocs = new Permission("READ", "documents", "Read documents");
        Permission writeDocs = new Permission("WRITE", "documents", "Write documents");

        adminRole.addPermission(readDocs);
        userRole.addPermission(writeDocs);

        manager.add(permanent(alice, adminRole));
        manager.add(permanent(alice, userRole));

        Set<Permission> perms = manager.getUserPermissions(alice);
        assertEquals(2, perms.size());
        assertTrue(perms.contains(readDocs));
        assertTrue(perms.contains(writeDocs));
    }

    @Test
    void getUserPermissions_excludesPermissionsFromInactiveRoles() {
        Permission readDocs = new Permission("READ", "documents", "Read documents");
        adminRole.addPermission(readDocs);

        manager.add(temporary(alice, adminRole, false)); // expired

        Set<Permission> perms = manager.getUserPermissions(alice);
        assertTrue(perms.isEmpty());
    }

    @Test
    void getUserPermissions_returnsEmpty_whenNoAssignments() {
        assertTrue(manager.getUserPermissions(alice).isEmpty());
    }

    @Test
    void getUserPermissions_throwsNPE_whenNull() {
        assertThrows(NullPointerException.class,
                () -> manager.getUserPermissions(null));
    }

    // endregion


    // region revokeAssignment

    @Test
    void revokeAssignment_removesAssignment() {
        PermanentAssignment a = permanent(alice, adminRole);
        manager.add(a);
        manager.revokeAssignment(a.assignmentId());
        assertEquals(0, manager.count());
    }

    @Test
    void revokeAssignment_throwsNoSuchElement_whenNotFound() {
        assertThrows(NoSuchElementException.class,
                () -> manager.revokeAssignment("nonexistent-id"));
    }

    @Test
    void revokeAssignment_throwsNPE_whenIdNull() {
        assertThrows(NullPointerException.class,
                () -> manager.revokeAssignment(null));
    }

    // endregion

    // region extendTemporaryAssignment

    @Test
    void extendTemporaryAssignment_updatesExpirationDate() {
        TemporaryAssignment temp = temporary(alice, adminRole, false); // currently expired
        manager.add(temp);

        String newDate = LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        manager.extendTemporaryAssignment(temp.assignmentId(), newDate);

        assertTrue(manager.userHasRole(alice, adminRole));
    }

    @Test
    void extendTemporaryAssignment_throwsNoSuchElement_whenNotFound() {
        String futureDate = LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        assertThrows(NoSuchElementException.class,
                () -> manager.extendTemporaryAssignment("nonexistent-id", futureDate));
    }

    @Test
    void extendTemporaryAssignment_throwsIllegalState_whenNotTemporary() {
        PermanentAssignment perm = permanent(alice, adminRole);
        manager.add(perm);

        String futureDate = LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        assertThrows(IllegalStateException.class,
                () -> manager.extendTemporaryAssignment(perm.assignmentId(), futureDate));
    }

    @Test
    void extendTemporaryAssignment_throwsNPE_whenIdNull() {
        String futureDate = LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        assertThrows(NullPointerException.class,
                () -> manager.extendTemporaryAssignment(null, futureDate));
    }

    @Test
    void extendTemporaryAssignment_throwsNPE_whenDateNull() {
        TemporaryAssignment temp = temporary(alice, adminRole, true);
        manager.add(temp);

        assertThrows(NullPointerException.class,
                () -> manager.extendTemporaryAssignment(temp.assignmentId(), null));
    }

    // endregion


    // region count

    @Test
    void count_returnsZero_initially() {
        assertEquals(0, manager.count());
    }

    @Test
    void count_incrementsAfterAdd() {
        manager.add(permanent(alice, adminRole));
        assertEquals(1, manager.count());
    }

    // endregion

    // region clear

    @Test
    void clear_removesAllAssignments() {
        manager.add(permanent(alice, adminRole));
        manager.add(permanent(bob, userRole));
        manager.clear();
        assertEquals(0, manager.count());
    }

    // endregion

    // region equals, hashCode, toString

    @Test
    void equals_returnsTrue_whenSameContent() {
        AssignmentManager other = new AssignmentManager(userManager, roleManager);
        PermanentAssignment a = permanent(alice, adminRole);
        manager.add(a);
        other.add(a);
        assertEquals(manager, other);
    }

    @Test
    void equals_returnsFalse_whenDifferentContent() {
        AssignmentManager other = new AssignmentManager(userManager, roleManager);
        manager.add(permanent(alice, adminRole));
        assertNotEquals(manager, other);
    }

    @Test
    void hashCode_equalsForSameContent() {
        AssignmentManager other = new AssignmentManager(userManager, roleManager);
        PermanentAssignment a = permanent(alice, adminRole);
        manager.add(a);
        other.add(a);
        assertEquals(manager.hashCode(), other.hashCode());
    }

    @Test
    void toString_containsCount() {
        manager.add(permanent(alice, adminRole));
        assertTrue(manager.toString().contains("1"));
    }

    // endregion
}
