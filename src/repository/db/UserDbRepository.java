package repository.db;

import domain.User;
import repository.UserRepository;
import validation.Validator;

import java.util.Optional;

public class UserDbRepository implements UserRepository {
    private final Validator<User> validator;
    private final String url;
    private final String user;
    private final String password;

    public UserDbRepository(Validator<User> validator, String url, String user, String password) {
        this.validator = validator;
        this.url = url;
        this.user = user;
        this.password = password;
    }

    @Override
    public Optional<User> findOne(Long aLong) {
        throw new UnsupportedOperationException(
                "Database persistence is not implemented yet"
        );
    }

    @Override
    public Iterable<User> findAll() {
        throw new UnsupportedOperationException(
                "Database persistence is not implemented yet"
        );
    }

    @Override
    public void save(User entity) {
        throw new UnsupportedOperationException(
                "Database persistence is not implemented yet"
        );
    }

    @Override
    public void delete(Long aLong) {
        throw new UnsupportedOperationException(
                "Database persistence is not implemented yet"
        );
    }

    @Override
    public void update(User entity) {
        throw new UnsupportedOperationException(
                "Database persistence is not implemented yet"
        );
    }
}
