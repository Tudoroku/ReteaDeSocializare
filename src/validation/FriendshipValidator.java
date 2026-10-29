
package validation;

import domain.Friendship;
import repository.UserRepository;

/**
 * Checks that a friendship is non-null and both users exist in the supplied
 * repository at validation time. ID normalization and structural checks belong
 * to the friendship constructor. Does not mutate either repository or entity.
 */
public class FriendshipValidator implements Validator<Friendship> {

    private final UserRepository userRepo;

    /**
     * Binds validation to the current contents of a user repository.
     * @param userRepo the non-null repository queried on each validation
     * @throws IllegalArgumentException if userRepo is null
     */
    public FriendshipValidator(UserRepository userRepo) {
        if (userRepo == null) {
            throw new IllegalArgumentException("User repository cannot be null!");
        }
        this.userRepo = userRepo;
    }

    /**
     * {@inheritDoc}
     * Looks up the second user only when the first exists.
     * @param entity the non-null friendship whose users must currently exist
     * @throws ValidationException if entity is null or either user is absent
     * @throws UnsupportedOperationException if user lookup is not implemented
     */
    @Override
    public void validate(Friendship entity) {
        if (entity == null) {
            throw new ValidationException("Friendship cannot be null!");
        }

        Long id1 = entity.getId().firstId();
        Long id2 = entity.getId().secondId();

        if (userRepo.findOne(id1).isEmpty() ||
                userRepo.findOne(id2).isEmpty()) {
            throw new ValidationException(
                    "Both users must exist to create a friendship!"
            );
        }
    }
}
