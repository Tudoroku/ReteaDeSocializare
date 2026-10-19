package service;

import domain.Friendship;
import domain.User;
import repository.FriendshipRepository;
import repository.UserRepository;
import validation.Validator;

import java.util.Objects;


public class UserService {
    private final UserRepository userRepo;
    private final Validator<User> validator;
    private final FriendshipRepository friendshipRepo;

    public UserService(UserRepository userRepo, Validator<User> validator, FriendshipRepository friendshipRepo) {
        this.userRepo = Objects.requireNonNull(userRepo);
        this.validator = Objects.requireNonNull(validator);
        this.friendshipRepo = Objects.requireNonNull(friendshipRepo);
    }

    public void addUser(User user)
    {
            validator.validate(user);
            userRepo.save(user);
    }

    public void deleteUser(Long id)
    {
        if(userRepo.findOne(id).isEmpty())
        {
            throw new IllegalArgumentException("User with ID: " + id + " doesn't exist");
        }

        var friendshipCollection = friendshipRepo.findAll();
        for(Friendship friendship : friendshipCollection)
        {
            var id1 = friendship.getId().firstId();
            var id2 = friendship.getId().secondId();
            if(id1.equals(id) || id2.equals(id))
            {
                friendshipRepo.delete(friendship.getId());
            }
        }
        userRepo.delete(id);
    }
}
