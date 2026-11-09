import domain.Friendship;
import repository.*;
import service.*;
import ui.*;
import validation.*;
import domain.User;
import domain.UserCriteria;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Application entry point that creates configured repositories, validators,
 * services and the console interface. The implicit public constructor has no
 * initialization behavior; startup is performed by {@link #main(String[])}.
 */
public class Main {
  /**
   * Builds the application from the configured storage type and runs its console UI.
   * Startup failures propagate before the console error handling is active.
   * @param args pass {@code --demo} to run the Lab 4 demo, or omit it to start the console UI
   * @throws repository.RepositoryException if configuration or initial file loading fails
   * @throws IllegalArgumentException if required configuration values or file paths are invalid
   * @throws java.util.NoSuchElementException if input ends while an operation reads a field
   * @throws IllegalStateException if the console scanner has been closed
   */
  public static void main(String[] args) {

    if (args != null && args.length == 1 && "--demo".equals(args[0])) {
      runLab4Demo();
      return;
    }
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

  /**
   * Demonstrates validator composition and repository filtering with sample users.
   */
  private static void runLab4Demo() {
    Validator<User> firstNameLengthRule = user -> {
      if (user.getFirstName().length() > 50) {
        throw new ValidationException("Prenumele nu poate depăși 50 de caractere.");
      }
    };
    Validator<User> composedValidator = new UserValidator().andThen(firstNameLengthRule);

    System.out.println("=== Demonstrație Lab 4 ===");
    System.out.println();
    System.out.println("1. Validarea utilizatorilor");

    User anaPopescu = new User(1L, "Ana", "Popescu");
    composedValidator.validate(anaPopescu);
    System.out.println("  Acceptat: Ana Popescu");

    System.out.println();
    try {
      composedValidator.validate(new User(4L, "A".repeat(51), "Popescu"));
    } catch (ValidationException exception) {
      System.out.println("  Respins: " + exception.getMessage());
    }

    UserRepository demoRepo = new InMemoryUserRepository();
    demoRepo.save(anaPopescu);
    demoRepo.save(new User(2L, "Ana", "Ionescu"));
    demoRepo.save(new User(3L, "Mihai", "Popescu"));

    Predicate<User> isAna = UserCriteria.hasFirstName("Ana");
    Predicate<User> isPopescu = UserCriteria.hasLastName("Popescu");

    System.out.println();
    System.out.println("2. Filtrarea utilizatorilor");

    printMatches("AND — prenume Ana și nume Popescu",
            demoRepo.findAll(isAna.and(isPopescu)));

    printMatches("OR — prenume Ana sau nume Popescu",
            demoRepo.findAll(isAna.or(isPopescu)));

    printMatches("negate() — alt prenume decât Ana",
            demoRepo.findAll(isAna.negate()));

    printMatches("Predicate.not() — alt prenume decât Ana",
            demoRepo.findAll(Predicate.not(isAna)));

    System.out.println();
    System.out.println("Nume de familie distincte:");

    demoRepo.findAll(user -> true).stream()
            .map(User::getLastName)
            .distinct()
            .sorted()
            .forEach(System.out::println);
  }

  /**
   * Prints a query title and its matching users, one per line.
   *
   * @param title the label describing the query
   * @param users the users matched by the query
   */
  private static void printMatches(String title, List<User> users) {
    System.out.println();
    System.out.println(title);

    if (users.isEmpty()) {
      System.out.println("  (niciun utilizator găsit)");
      return;
    }

    users.stream()
            .sorted(Comparator.comparing(User::getLastName)
                    .thenComparing(User::getFirstName))
            .map(Main::formatUser)
            .forEach(System.out::println);
  }
  /**
   * Formats a user as a bullet for the demo output.
   *
   * @param user the user to display
   * @return the bullet containing the user's first and last names
   */
  private static String formatUser(User user) {
    return "  - " + user.getFirstName() + " " + user.getLastName();
  }
}
