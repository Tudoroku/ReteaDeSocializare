package repository;

import domain.Friendship;
import domain.FriendshipId;

/**
 * A friendship repository with the reference-storage behavior of {@link InMemoryRepository}.
 * The implicit public constructor creates an empty instance. Does not check user
 * existence or persist data.
 */
public class InMemoryFriendshipRepository extends InMemoryRepository<FriendshipId, Friendship> implements FriendshipRepository{

}
