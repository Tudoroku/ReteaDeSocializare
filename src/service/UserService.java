package service;

import domain.Friendship;
import domain.User;
import repository.FriendshipRepository;
import repository.UserRepository;
import validation.Validator;
import repository.RepositoryException;

import java.util.Objects;


/**
 * Validates user insertion and coordinates user deletion with friendship cleanup.
 * Repositories perform their own persistence. Multi-repository operations are
 * sequential and have no service-level rollback.
 */
public class UserService {
    private final UserRepository userRepo;
    private final Validator<User> validator;
    private final FriendshipRepository friendshipRepo;

    /**
     * Binds the repositories and insertion validator.
     * @param userRepo the non-null user repository
     * @param validator the non-null validator called before inserting users
     * @param friendshipRepo the non-null repository used for friendship cleanup
     * @throws NullPointerException if any dependency is null
     */
    public UserService(UserRepository userRepo, Validator<User> validator, FriendshipRepository friendshipRepo) {
        this.userRepo = Objects.requireNonNull(userRepo);
        this.validator = Objects.requireNonNull(validator);
        this.friendshipRepo = Objects.requireNonNull(friendshipRepo);
    }

    /**
     * Validates the supplied user, then delegates insertion to the user repository.
     * A FILE repository may validate the user again. Validation failure prevents
     * the insertion call; persistence and duplicate handling belong to the repository.
     * @param user the user accepted by the configured validator and repository
     * @throws validation.ValidationException if validation rejects the user
     * @throws IllegalArgumentException if repository argument or duplicate checks fail
     * @throws repository.RepositoryException if repository persistence fails
     * @throws UnsupportedOperationException if insertion is not implemented
     */
    public void addUser(User user)
    {
            validator.validate(user);
            userRepo.save(user);
    }

    /**
     * Deletes a user after deleting all friendships involving that user.
     * If a deletion fails, any friendships already deleted are not restored.
     *
     * @param id the identifier of the user to delete
     * @throws IllegalArgumentException if the user does not exist
     * @throws RepositoryException if a deletion cannot be persisted
     * @throws UnsupportedOperationException if a repository operation is unsupported
     */
    public void deleteUser(Long id) {
        if (userRepo.findOne(id).isEmpty()) {
            throw new IllegalArgumentException("User with ID: " + id + " doesn't exist");
        }

        friendshipRepo.findAll(friendship ->
                        friendship.getId().firstId().equals(id)
                                || friendship.getId().secondId().equals(id))
                .stream()
                .map(Friendship::getId)
                .forEach(friendshipRepo::delete);

        userRepo.delete(id);
    }
}
