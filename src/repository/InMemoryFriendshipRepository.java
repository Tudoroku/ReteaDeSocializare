package repository;

import domain.Friendship;
import domain.FriendshipId;

public class InMemoryFriendshipRepository extends InMemoryRepository<FriendshipId, Friendship> implements FriendshipRepository{

}
