package repository;

import domain.User;

public class InMemoryUserRepository extends InMemoryRepository<Long, User> implements UserRepository {
}
