import domain.Friendship;
import repository.*;
import service.*;
import ui.*;
import validation.*;

public class Main {
  public static void main(String[] args) {

    // repos and validators
    RepositoryFactory repoFactory = new RepositoryFactory();

    UserValidator userValidator = new UserValidator();
    UserRepository userRepo = repoFactory.createUserRepository(userValidator);

    Validator<Friendship> friendshipValidator = new FriendshipValidator(userRepo);
    FriendshipRepository friendshipRepo = repoFactory.createFriendshipRepository(friendshipValidator);

    // services
    UserService userService = new UserService(
            userRepo, userValidator, friendshipRepo
    );

    FriendshipService friendshipService = new FriendshipService(
            userRepo, friendshipRepo
    );

    CommunityService communityService = new CommunityService(
            userRepo, friendshipRepo
    );

    // ui
    UI ui = new ConsoleUI(
            communityService,
            friendshipService,
            userService
    );

    ui.start();
  }
}
