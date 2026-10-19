package service;

import repository.FriendshipRepository;
import repository.UserRepository;

public class CommunityService {
    private final UserRepository userRepo;
    private final FriendshipRepository friendshipRepo;

    public CommunityService(UserRepository userRepo, FriendshipRepository friendshipRepo) {
        this.userRepo = userRepo;
        this.friendshipRepo = friendshipRepo;
    }
}
