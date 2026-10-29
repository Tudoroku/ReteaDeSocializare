package service;

import domain.Friendship;
import domain.FriendshipId;
import repository.FriendshipRepository;
import repository.UserRepository;

/**
 * Constructs normalized friendships, checks participating users on insertion,
 * and delegates storage and deletion to the friendship repository.
 */
public class FriendshipService {
    private final UserRepository userRepo;
    private final FriendshipRepository friendshipRepo;

    /**
     * Stores dependencies without checking them for null.
     * @param userRepo the repository queried for participating users
     * @param friendshipRepo the repository used for friendship mutations
     */
    public FriendshipService(UserRepository userRepo, FriendshipRepository friendshipRepo) {
        this.userRepo = userRepo;
        this.friendshipRepo = friendshipRepo;
    }

    /**
     * Constructs a friendship dated now, checks both users, then delegates insertion.
     * The service itself does not invoke a validator; a FILE repository validates
     * again before storing. Reversed IDs identify the same friendship.
     * @param id1 one non-null, positive user ID, distinct from id2
     * @param id2 the other non-null, positive user ID
     * @throws IllegalArgumentException if IDs are invalid, a user is absent, or
     *         the repository already contains the friendship
     * @throws validation.ValidationException if the repository's validator rejects the friendship
     * @throws repository.RepositoryException if repository persistence fails
     * @throws UnsupportedOperationException if a delegated operation is not implemented
     * @throws NullPointerException if a required repository dependency is null
     */
    public void addFriendship(Long id1, Long id2)
    {
        Friendship friendship = new Friendship(id1, id2);
        if (userRepo.findOne(id1).isEmpty() || userRepo.findOne(id2).isEmpty())
        {
            throw new IllegalArgumentException("Users not found");
        }
        friendshipRepo.save(friendship);
    }

    /**
     * Delegates deletion using a normalized pair, without checking user existence.
     * @param id1 one non-null, positive user ID, distinct from id2
     * @param id2 the other non-null, positive user ID
     * @throws IllegalArgumentException if IDs are invalid or the friendship is absent
     * @throws repository.RepositoryException if repository persistence fails
     * @throws UnsupportedOperationException if deletion is not implemented
     * @throws NullPointerException if the friendship repository dependency is null
     */
    public void deleteFriendship(Long id1, Long id2)
    {
        friendshipRepo.delete(new FriendshipId(id1, id2));
    }
}
