import domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import repository.db.UserDbRepository;
import validation.UserValidator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Uses existing public tables; never loads application configuration or changes DDL. */
abstract class JdbcTestSupport {
    protected String url;
    protected String username;
    protected String password;
    protected UserDbRepository users;
    private final List<User> ownedUsers = new ArrayList<>();

    @BeforeEach
    void configureTestDatabase() throws SQLException {
        String configuredUrl = System.getenv("SOCIALNETWORK_TEST_DB_URL");
        username = System.getenv("SOCIALNETWORK_TEST_DB_USER");
        password = System.getenv("SOCIALNETWORK_TEST_DB_PASSWORD");
        assumeTrue(configuredUrl != null && !configuredUrl.isBlank()
                        && username != null && !username.isBlank() && password != null,
                "JDBC integration skipped: set SOCIALNETWORK_TEST_DB_URL, "
                        + "SOCIALNETWORK_TEST_DB_USER and SOCIALNETWORK_TEST_DB_PASSWORD");
        // Deliberately disallow URL properties, credentials, alternate databases and host lists.
        assertTrue(configuredUrl.matches(
                        "jdbc:postgresql://[A-Za-z0-9._-]+(?::[0-9]+)?/socialnetwork_test"),
                "Refusing unsafe test URL: expected jdbc:postgresql://host[:port]/socialnetwork_test");
        url = configuredUrl + "?currentSchema=public&connectTimeout=5&socketTimeout=10";
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("SELECT current_database()");
             var rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals("socialnetwork_test", rs.getString(1), "Refusing non-test database");
        }
        users = new UserDbRepository(new UserValidator(), url, username, password);
    }

    protected Connection connection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    protected User track(User user) {
        ownedUsers.add(user); // Track before save, so later assertion failures still clean up.
        return user;
    }

    protected User createUser() {
        User user = track(new User("Jdbc", "Test"));
        users.save(user);
        return user;
    }

    protected Long missingUserId() {
        User user = createUser();
        users.delete(user.getId());
        return user.getId();
    }

    @AfterEach
    void cleanOwnedRows() throws SQLException {
        if (ownedUsers.isEmpty()) return;
        // Explicit friendship cleanup also works when the cascade-under-test is missing.
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try (PreparedStatement friendships = c.prepareStatement(
                    "DELETE FROM friendships WHERE id1 = ? OR id2 = ?");
                 PreparedStatement people = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
                for (User user : ownedUsers) {
                    if (user.getId() == null) continue;
                    friendships.setLong(1, user.getId());
                    friendships.setLong(2, user.getId());
                    friendships.executeUpdate();
                    people.setLong(1, user.getId());
                    people.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    protected static void assertUser(User expected, User actual) {
        // User.equals compares IDs only.
        assertAll(() -> assertEquals(expected.getId(), actual.getId()),
                () -> assertEquals(expected.getFirstName(), actual.getFirstName()),
                () -> assertEquals(expected.getLastName(), actual.getLastName()));
    }
}
