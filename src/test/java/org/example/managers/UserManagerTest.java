package org.example.managers;

import org.example.User;
import org.example.filter.UserFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserManagerTest {

    private UserManager manager;

    @BeforeEach
    void setUp() {
        manager = new UserManager();
    }

    private User user(String username) {
        return User.create(username, "Full Name", username + "@example.com");
    }

    // region add

    @Test
    void add_addsUserSuccessfully() {
        manager.add(user("Alice"));
        assertEquals(1, manager.count());
    }

    @Test
    void add_throwsNPE_whenNull() {
        assertThrows(NullPointerException.class, () -> manager.add(null));
    }

    @Test
    void add_throwsIllegalState_whenDuplicateUsername() {
        manager.add(user("Alice"));
        assertThrows(IllegalStateException.class, () -> manager.add(user("Alice")));
    }

    // endregion

    // region remove

    @Test
    void remove_returnsTrue_whenUserExists() {
        User u = user("Alice");
        manager.add(u);
        assertTrue(manager.remove(u));
        assertEquals(0, manager.count());
    }

    @Test
    void remove_returnsFalse_whenNull() {
        assertFalse(manager.remove(null));
    }

    @Test
    void remove_returnsFalse_whenUserNotRegistered() {
        assertFalse(manager.remove(user("Alice")));
    }

    // endregion

    // region update

    @Test
    void update_updatesFullNameAndEmail() {
        manager.add(user("Alice"));
        manager.update("Alice", "New Name", "new@example.com");

        User updated = manager.findByUsername("Alice").orElseThrow();
        assertEquals("New Name", updated.fullName());
        assertEquals("new@example.com", updated.email());
    }

    @Test
    void update_throwsNoSuchElement_whenUserNotFound() {
        assertThrows(NoSuchElementException.class,
                () -> manager.update("nobody", "Name", "email@example.com"));
    }

    // endregion


    // region findAll

    @Test
    void findAll_returnsAllUsers() {
        manager.add(user("Alice"));
        manager.add(user("bob_99"));
        assertEquals(2, manager.findAll().size());
    }

    @Test
    void findAll_returnsEmptyList_initially() {
        assertTrue(manager.findAll().isEmpty());
    }

    @Test
    void findAll_withFilterAndSorter_returnsFilteredSortedList() {
        manager.add(user("charlie"));
        manager.add(user("Alice"));
        manager.add(user("bob_99"));

        List<User> result = manager.findAll(_ -> true, Comparator.comparing(User::username));

        assertEquals(3, result.size());
        assertEquals("Alice", result.get(0).username());
        assertEquals("bob_99", result.get(1).username());
        assertEquals("charlie", result.get(2).username());
    }

    @Test
    void findAll_withFilter_returnsOnlyMatching() {
        manager.add(user("Alice"));
        manager.add(user("bob_99"));

        List<User> result = manager.findAll(
                u -> u.username().startsWith("b"),
                Comparator.comparing(User::username)
        );

        assertEquals(1, result.size());
        assertEquals("bob_99", result.getFirst().username());
    }

    @Test
    void findAll_throwsNPE_whenFilterNull() {
        assertThrows(NullPointerException.class,
                () -> manager.findAll(null, Comparator.comparing(User::username)));
    }

    @Test
    void findAll_throwsNPE_whenSorterNull() {
        assertThrows(NullPointerException.class,
                () -> manager.findAll(_ -> true, null));
    }

    // endregion

    // region findById

    @Test
    void findById_returnsUser_whenExists() {
        User u = user("Alice");
        manager.add(u);
        Optional<User> result = manager.findById("Alice");
        assertTrue(result.isPresent());
        assertEquals(u, result.get());
    }

    @Test
    void findById_returnsEmpty_whenNotFound() {
        assertTrue(manager.findById("nobody").isEmpty());
    }

    // endregion

    // region findByUsername

    @Test
    void findByUsername_returnsUser_whenExists() {
        manager.add(user("Alice"));
        assertTrue(manager.findByUsername("Alice").isPresent());
    }

    @Test
    void findByUsername_returnsEmpty_whenNotFound() {
        assertTrue(manager.findByUsername("nobody").isEmpty());
    }

    // endregion

    // region findByEmail

    @Test
    void findByEmail_returnsUser_caseInsensitive() {
        manager.add(User.create("Alice", "Alice Smith", "Alice@Example.COM"));
        assertTrue(manager.findByEmail("Alice@example.com").isPresent());
    }

    @Test
    void findByEmail_returnsEmpty_whenNull() {
        assertTrue(manager.findByEmail(null).isEmpty());
    }

    @Test
    void findByEmail_returnsEmpty_whenEmailNotFound() {
        assertTrue(manager.findByEmail("unknown@example.com").isEmpty());
    }

    // endregion

    // region findByFilter

    @Test
    void findByFilter_returnsMatchingUsers() {
        manager.add(user("Alice"));
        manager.add(user("bob_99"));

        UserFilter filter = u -> u.username().startsWith("A");
        List<User> result = manager.findByFilter(filter);

        assertEquals(1, result.size());
        assertEquals("Alice", result.getFirst().username());
    }

    @Test
    void findByFilter_returnsEmpty_whenNoMatch() {
        manager.add(user("Alice"));
        List<User> result = manager.findByFilter(u -> u.username().startsWith("z"));
        assertTrue(result.isEmpty());
    }

    @Test
    void findByFilter_throwsNPE_whenNull() {
        assertThrows(NullPointerException.class, () -> manager.findByFilter(null));
    }

    // endregion


    // region count

    @Test
    void count_returnsZero_initially() {
        assertEquals(0, manager.count());
    }

    @Test
    void count_incrementsAfterAdd() {
        manager.add(user("Alice"));
        assertEquals(1, manager.count());
    }

    // endregion

    // region exists

    @Test
    void exists_returnsTrue_whenUserAdded() {
        manager.add(user("Alice"));
        assertTrue(manager.exists("Alice"));
    }

    @Test
    void exists_returnsFalse_whenUserAbsent() {
        assertFalse(manager.exists("nobody"));
    }

    // endregion

    // region equals, hashCode, toString

    @Test
    void equals_returnsTrue_whenSameContent() {
        UserManager other = new UserManager();
        User u = user("Alice");
        manager.add(u);
        other.add(u);
        assertEquals(manager, other);
    }

    @Test
    void equals_returnsFalse_whenDifferentContent() {
        UserManager other = new UserManager();
        manager.add(user("Alice"));
        other.add(user("bob_99"));
        assertNotEquals(manager, other);
    }

    @Test
    void hashCode_equalsForSameContent() {
        UserManager other = new UserManager();
        User u = user("Alice");
        manager.add(u);
        other.add(u);
        assertEquals(manager.hashCode(), other.hashCode());
    }

    @Test
    void toString_containsCount() {
        manager.add(user("Alice"));
        assertTrue(manager.toString().contains("1"));
    }

    // endregion

}
