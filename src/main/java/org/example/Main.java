import org.example.User;

void testUserCreation() {
    User valid = User.create("john_doe", "John Doe", "john@example.com");

    expectError("null username", () -> User.create(null, "John Doe", "john@example.com"));
    expectError("blank username", () -> User.create("  ", "John Doe", "john@example.com"));
    expectError("invalid username", () -> User.create("john doe!", "John Doe", "john@example.com"));
    expectError("short username", () -> User.create("ab", "John Doe", "john@example.com"));
    expectError("invalid email", () -> User.create("john_doe", "John Doe", "not-an-email"));
    expectError("blank fullName", () -> User.create("john_doe", "", "john@example.com"));
}

void main() {
    testUserCreation();
}

void expectError(String label, Runnable action) {
    try {
        action.run();
        System.out.println(label + ": исключение не выброшено");
    } catch (Exception e) {
        System.out.println(label + ": " + e.getMessage());
    }
}
