import repository.*;
import service.*;
import ui.*;
import validation.*;

public class Main {
  public static void main(String[] args) {

    // repos
    UserRepository userRepo = new InMemoryUserRepository();
    FriendshipRepository friendshipRepo = new InMemoryFriendshipRepository();

    // validator
    UserValidator userValidator = new UserValidator();

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
