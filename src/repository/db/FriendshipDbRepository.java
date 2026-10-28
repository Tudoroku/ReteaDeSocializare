package repository.db;

import domain.Friendship;
import domain.FriendshipId;
import repository.FriendshipRepository;
import validation.Validator;

import java.util.Optional;

public class FriendshipDbRepository implements FriendshipRepository {
    private final Validator<Friendship> validator;
    private final String url;
    private final String user;
    private final String password;

    public FriendshipDbRepository(Validator<Friendship> validator, String url, String user, String password) {
        this.validator = validator;
        this.url = url;
        this.user = user;
        this.password = password;
    }

    @Override
    public Optional<Friendship> findOne(FriendshipId friendshipId) {
        throw new UnsupportedOperationException(
                "Database persistence is not implemented yet"
        );
    }

    @Override
    public Iterable<Friendship> findAll() {
        throw new UnsupportedOperationException(
                "Database persistence is not implemented yet"
        );
    }

    @Override
    public void save(Friendship entity) {
        throw new UnsupportedOperationException(
                "Database persistence is not implemented yet"
        );
    }

    @Override
    public void delete(FriendshipId friendshipId) {
        throw new UnsupportedOperationException(
                "Database persistence is not implemented yet"
        );
    }

    @Override
    public void update(Friendship entity) {
        throw new UnsupportedOperationException(
                "Database persistence is not implemented yet"
        );
    }
}
