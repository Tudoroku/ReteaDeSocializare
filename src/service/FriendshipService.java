package service;

import domain.Friendship;
import domain.FriendshipId;
import repository.FriendshipRepository;
import repository.UserRepository;

public class FriendshipService {
    private final UserRepository userRepo;
    private final FriendshipRepository friendshipRepo;

    public FriendshipService(UserRepository userRepo, FriendshipRepository friendshipRepo) {
        this.userRepo = userRepo;
        this.friendshipRepo = friendshipRepo;
    }

    public void addFriendship(Long id1, Long id2)
    {
        Friendship friendship = new Friendship(id1, id2);
        if (userRepo.findOne(id1).isEmpty() || userRepo.findOne(id2).isEmpty())
        {
            throw new IllegalArgumentException("Users not found");
        }
        friendshipRepo.save(friendship);
    }

    public void deleteFriendship(Long id1, Long id2)
    {
        friendshipRepo.delete(new FriendshipId(id1, id2));
    }
}
