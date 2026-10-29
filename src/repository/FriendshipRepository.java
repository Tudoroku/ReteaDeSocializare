package repository;

import domain.Friendship;
import domain.FriendshipId;

/** Repository specialization for friendships identified by normalized user pairs. */
public interface FriendshipRepository extends Repository<FriendshipId, Friendship>{
}
