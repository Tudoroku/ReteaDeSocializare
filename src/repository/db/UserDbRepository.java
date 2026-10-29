package repository.db;

import domain.User;
import repository.UserRepository;
import validation.Validator;

import java.util.Optional;

/**
 * Placeholder for user database persistence. Stores constructor arguments only;
 * no connection, validation or CRUD operation is implemented.
 */
public class UserDbRepository implements UserRepository {
    private final Validator<User> validator;
    private final String url;
    private final String user;
    private final String password;

    /**
     * Stores database settings without checking them or opening a connection.
     * @param validator the validator reserved for future use; may be null
     * @param url the database URL reserved for future use; may be null
     * @param user the database username reserved for future use; may be null
     * @param password the database password reserved for future use; may be null
     */
    public UserDbRepository(Validator<User> validator, String url, String user, String password) {
        this.validator = validator;
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Rejects lookup because database persistence is not implemented.
     * @param aLong the ignored identifier, including null
     * @return no result; this method always throws
     * @throws UnsupportedOperationException always
     */
    @Override
    public Optional<User> findOne(Long aLong) {
        throw new UnsupportedOperationException(
                "Database persistence is not implemented yet"
        );
    }

    /**
     * Rejects retrieval because database persistence is not implemented.
     * @return no iterable; this method always throws
     * @throws UnsupportedOperationException always
     */
    @Override
    public Iterable<User> findAll() {
        throw new UnsupportedOperationException(
                "Database persistence is not implemented yet"
        );
    }

    /**
     * Rejects insertion before performing any validation or storage.
     * @param entity the ignored entity, including null
     * @throws UnsupportedOperationException always
     */
    @Override
    public void save(User entity) {
        throw new UnsupportedOperationException(
                "Database persistence is not implemented yet"
        );
    }

    /**
     * Rejects deletion without examining the identifier.
     * @param aLong the ignored identifier, including null
     * @throws UnsupportedOperationException always
     */
    @Override
    public void delete(Long aLong) {
        throw new UnsupportedOperationException(
                "Database persistence is not implemented yet"
        );
    }

    /**
     * Rejects replacement before performing any validation or storage.
     * @param entity the ignored replacement, including null
     * @throws UnsupportedOperationException always
     */
    @Override
    public void update(User entity) {
        throw new UnsupportedOperationException(
                "Database persistence is not implemented yet"
        );
    }
}
