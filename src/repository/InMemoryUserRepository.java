package repository;

import domain.User;

/**
 * A user repository with the reference-storage behavior of {@link InMemoryRepository}.
 * The implicit public constructor creates an empty instance. Does not run user
 * validation or persist data.
 */
public class InMemoryUserRepository extends InMemoryRepository<Long, User> implements UserRepository {
}
