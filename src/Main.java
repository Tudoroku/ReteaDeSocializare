import domain.Friendship;
import repository.*;
import service.*;
import ui.*;
import validation.*;

/**
 * Application entry point that creates configured repositories, validators,
 * services and the console interface. The implicit public constructor has no
 * initialization behavior; startup is performed by {@link #main(String[])}.
 */
public class Main {
  /**
   * Builds the application from the configured storage type and runs its console UI.
   * Startup failures propagate before the console error handling is active.
   * @param args unused command-line arguments; null is accepted
   * @throws repository.RepositoryException if configuration or initial file loading fails
   * @throws IllegalArgumentException if required configuration values or file paths are invalid
   * @throws java.util.NoSuchElementException if input ends while an operation reads a field
   * @throws IllegalStateException if the console scanner has been closed
   */
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
