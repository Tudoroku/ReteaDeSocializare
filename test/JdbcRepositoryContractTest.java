import domain.Friendship;
import domain.FriendshipId;
import domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import repository.RepositoryException;
import repository.db.FriendshipDbRepository;
import repository.db.UserDbRepository;
import validation.UserValidator;
import validation.ValidationException;

import java.sql.SQLException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Argument checks and real JDBC connection failures need no configured database. */
class JdbcRepositoryContractTest {
    // Loopback port 1 is not a PostgreSQL service. No application credentials are read.
    private static final String UNREACHABLE =
            "jdbc:postgresql://127.0.0.1:1/socialnetwork_test?connectTimeout=1&socketTimeout=1";

    private static UserDbRepository users() {
        return new UserDbRepository(new UserValidator(), UNREACHABLE, "unused", "");
    }

    private static FriendshipDbRepository friendships() {
        // Allow the operation to reach JDBC without an earlier user-existence lookup.
        return new FriendshipDbRepository(f -> {}, UNREACHABLE, "unused", "");
    }

    @Test
    void userConstructorRejectsNullDependenciesWithoutConnecting() {
        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> new UserDbRepository(null, UNREACHABLE, "unused", "")),
                () -> assertThrows(NullPointerException.class,
                        () -> new UserDbRepository(new UserValidator(), null, "unused", "")),
                () -> assertThrows(NullPointerException.class,
                        () -> new UserDbRepository(new UserValidator(), UNREACHABLE, null, "")),
                () -> assertThrows(NullPointerException.class,
                        () -> new UserDbRepository(new UserValidator(), UNREACHABLE, "unused", null)));
    }

    @Test
    void userNullArgumentsAreRejectedBeforeConnecting() {
        var repo = users();
        assertAll(() -> assertThrows(IllegalArgumentException.class, () -> repo.findOne(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> repo.save(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> repo.update(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> repo.delete(null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> repo.update(new User("First", "Last"))));
    }

    @Test
    void friendshipNullArgumentsAreRejectedBeforeConnecting() {
        var repo = friendships();
        assertAll(() -> assertThrows(IllegalArgumentException.class, () -> repo.findOne(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> repo.save(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> repo.update(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> repo.delete(null)));
    }

    @Test
    void requiredInjectionNamePassesProductionValidationAndReachesJdbc() {
        String payload = "'); DROP TABLE users; --";
        var candidate = new User(payload, "Last");
        assertDoesNotThrow(() -> new UserValidator().validate(candidate));
        RepositoryException error = assertThrows(RepositoryException.class, () -> users().save(candidate));
        SQLException cause = assertInstanceOf(SQLException.class, error.getCause());
        assertTrue(cause.getSQLState().startsWith("08"));
        assertTrue(error.getMessage().contains("Database connection unavailable while saving user"));
        assertNull(candidate.getId());
        assertDoesNotThrow(() -> new UserValidator().validate(new User("First", payload)));
    }

    @Test
    void invalidIdsAndNamesAreRejectedBeforeConnecting() {
        var repo = users();
        assertThrows(IllegalArgumentException.class, () -> repo.save(new User(1L, "First", "Last")));
        assertThrows(ValidationException.class, () -> repo.update(new User(0L, "First", "Last")));
        assertThrows(ValidationException.class, () -> repo.update(new User(-1L, "First", "Last")));
        assertThrows(ValidationException.class, () -> repo.save(new User(null, "Last")));
        assertThrows(ValidationException.class, () -> repo.save(new User("First", " ")));
    }

    static Stream<Object[]> failingOperations() {
        var people = users();
        var relations = friendships();
        var id = new FriendshipId(1L, 2L);
        return Stream.of(
                new Object[]{"user findOne", (Executable) () -> people.findOne(1L), "finding user"},
                new Object[]{"user findAll", (Executable) people::findAll, "retrieving users"},
                new Object[]{"user save", (Executable) () -> people.save(new User("First", "Last")), "saving user"},
                new Object[]{"user update", (Executable) () -> people.update(new User(1L, "First", "Last")), "updating user"},
                new Object[]{"user delete", (Executable) () -> people.delete(1L), "deleting user"},
                new Object[]{"friendship findOne", (Executable) () -> relations.findOne(id), "finding friendship"},
                new Object[]{"friendship findAll", (Executable) relations::findAll, "retrieving friendships"},
                // save checks duplicates via findOne, so connection failure occurs at lookup.
                new Object[]{"friendship save", (Executable) () -> relations.save(new Friendship(1L, 2L)), "finding friendship"},
                new Object[]{"friendship update", (Executable) () -> relations.update(new Friendship(1L, 2L)), "updating friendship"},
                new Object[]{"friendship delete", (Executable) () -> relations.delete(id), "deleting friendship"});
    }

    @ParameterizedTest(name = "{0} preserves the JDBC connection failure")
    @MethodSource("failingOperations")
    void connectionFailuresPreserveSqlException(String operation, Executable action, String context) {
        RepositoryException error = assertThrows(RepositoryException.class, action);
        SQLException cause = assertInstanceOf(SQLException.class, error.getCause());
        assertNotNull(cause.getSQLState());
        assertTrue(cause.getSQLState().startsWith("08"), operation);
        assertFalse(cause.getMessage().isBlank());
        assertTrue(error.getMessage().contains(context), error.getMessage());
        assertTrue(error.getMessage().startsWith("Database connection unavailable while "));
        assertFalse(error.getMessage().contains(cause.getMessage()));
        assertFalse(error.getMessage().contains(UNREACHABLE));
    }
}
