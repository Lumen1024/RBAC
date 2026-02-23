package org.example.managers;

import org.example.User;
import org.example.filter.UserFilter;

import java.util.*;
import java.util.stream.Collectors;

public class UserManager implements Repository<User> {

    private final Map<String, User> users = new HashMap<>();

    @Override
    public void add(User item) {
        Objects.requireNonNull(item, "Пользователь не может быть null");
        if (users.containsKey(item.username())) {
            throw new IllegalStateException(
                    "Пользователь с username '%s' уже существует".formatted(item.username()));
        }
        users.put(item.username(), item);
    }

    @Override
    public boolean remove(User item) {
        if (item == null) return false;
        return users.remove(item.username(), item);
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(users.values());
    }

    @Override
    public int count() {
        return users.size();
    }

    @Override
    public void clear() {
        users.clear();
    }


    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return users.values().stream()
                .filter(u -> u.email().equalsIgnoreCase(email))
                .findFirst();
    }

    public List<User> findByFilter(UserFilter filter) {
        Objects.requireNonNull(filter, "Фильтр не может быть null");

        return users.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        Objects.requireNonNull(filter, "Фильтр не может быть null");
        Objects.requireNonNull(sorter, "Сортировщик не может быть null");

        return users.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public boolean exists(String username) {
        return users.containsKey(username);
    }

    public void update(String username, String newFullName, String newEmail) {
        if (!users.containsKey(username)) {
            throw new NoSuchElementException(
                    "Пользователь с username '%s' не найден".formatted(username));
        }
        users.put(username, User.create(username, newFullName, newEmail));
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserManager that)) return false;
        return users.equals(that.users);
    }

    @Override
    public int hashCode() {
        return Objects.hash(users);
    }

    @Override
    public String toString() {
        return "UserManager{count=%d}".formatted(users.size());
    }
}
