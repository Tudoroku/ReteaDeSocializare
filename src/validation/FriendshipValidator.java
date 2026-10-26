
package validation;

import domain.Friendship;
import repository.UserRepository;

public class FriendshipValidator implements Validator<Friendship> {

    private final UserRepository userRepo;

    public FriendshipValidator(UserRepository userRepo) {
        if (userRepo == null) {
            throw new IllegalArgumentException("User repository cannot be null!");
        }
        this.userRepo = userRepo;
    }

    @Override
    public void validate(Friendship entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Friendship cannot be null!");
        }

        Long id1 = entity.getId().firstId();
        Long id2 = entity.getId().secondId();

        if (userRepo.findOne(id1).isEmpty() ||
                userRepo.findOne(id2).isEmpty()) {
            throw new IllegalArgumentException(
                    "Both users must exist to create a friendship!"
            );
        }
    }
}
