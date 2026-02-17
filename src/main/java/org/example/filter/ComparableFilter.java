package org.example.filter;

@FunctionalInterface
public interface  ComparableFilter<T> {
    boolean test(T item);

    default ComparableFilter<T> and(ComparableFilter<T> other) {
        return user -> test(user) && other.test(user);
    }

    default ComparableFilter<T> or(ComparableFilter<T> other) {
        return user -> test(user) || other.test(user);
    }
}
