import domain.User;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import repository.RepositoryException;
import repository.db.UserDbRepository;
import repository.db.FriendshipDbRepository;
import service.UserService;
import validation.FriendshipValidator;
import validation.UserValidator;
import validation.ValidationException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@Tag("jdbc")
class UserDbRepositoryIntegrationTest extends JdbcTestSupport {
    @Test
    void saveGeneratesPositiveDistinctIdsAndAssignsThemToObjects() {
        assertTrue(users.generatesIds());
        User first = createUser();
        User second = createUser();
        assertNotNull(first.getId());
        assertTrue(first.getId() > 0);
        assertTrue(second.getId() > 0);
        assertNotEquals(first.getId(), second.getId());
        assertUser(first, users.findOne(first.getId()).orElseThrow());
        assertUser(second, users.findOne(second.getId()).orElseThrow());
    }

    @Test
    void findOneReturnsEmptyForMissingId() {
        assertTrue(users.findOne(missingUserId()).isEmpty());
    }

    @Test
    void findAllContainsPersistedUsers() {
        User first = createUser();
        User second = createUser();
        var found = users.findAll(u -> u.getId().equals(first.getId()) || u.getId().equals(second.getId()));
        assertEquals(2, found.size());
        assertUser(first, found.stream().filter(u -> u.getId().equals(first.getId())).findFirst().orElseThrow());
        assertUser(second, found.stream().filter(u -> u.getId().equals(second.getId())).findFirst().orElseThrow());
    }

    @Test
    void updatePersistsBothNamesAndPreservesOtherUsers() {
        User original = createUser();
        User other = createUser();
        User replacement = new User(original.getId(), "Updated", "O'Connor");
        assertTrue(users.update(replacement).isEmpty());
        assertUser(replacement, users.findOne(original.getId()).orElseThrow());
        assertUser(other, users.findOne(other.getId()).orElseThrow());
    }

    @Test
    void updateMissingReturnsSameEntityWithoutInserting() {
        User missing = new User(missingUserId(), "Missing", "User");
        assertSame(missing, users.update(missing).orElseThrow());
        assertTrue(users.findOne(missing.getId()).isEmpty());
    }

    @Test
    void deleteReturnsFullEntityAndThenEmpty() {
        User original = createUser();
        User other = createUser();
        assertUser(original, users.delete(original.getId()).orElseThrow());
        assertTrue(users.findOne(original.getId()).isEmpty());
        assertTrue(users.delete(original.getId()).isEmpty());
        assertUser(other, users.findOne(other.getId()).orElseThrow());
    }

    @Test
    void rejectsAssignedIdOnSave() {
        User existing = createUser();
        assertThrows(IllegalArgumentException.class, () -> users.save(existing));
        assertUser(existing, users.findOne(existing.getId()).orElseThrow());
    }

    @Test
    void invalidNamesAreRejectedOnSaveAndUpdateWithoutChangingRows() {
        User original = createUser();
        String[][] invalid = {{null, "Last"}, {"", "Last"}, {"  ", "Last"},
                {"First", null}, {"First", ""}, {"First", "\t"}};
        for (String[] names : invalid) {
            User candidate = track(new User(names[0], names[1]));
            assertThrows(ValidationException.class, () -> users.save(candidate));
            assertNull(candidate.getId());
            assertThrows(ValidationException.class,
                    () -> users.update(new User(original.getId(), names[0], names[1])));
            assertUser(original, users.findOne(original.getId()).orElseThrow());
        }
    }

    @Test
    void databaseConstraintFailurePreservesSqlCause() {
        // Intentionally bypass application validation to exercise the real PostgreSQL NOT NULL constraint.
        var jdbcOnly = new UserDbRepository(u -> {}, url, username, password);
        User invalid = track(new User(null, "Last"));
        RepositoryException error = assertThrows(RepositoryException.class, () -> jdbcOnly.save(invalid));
        SQLException cause = assertInstanceOf(SQLException.class, error.getCause());
        assertEquals("23502", cause.getSQLState());
        assertTrue(error.getMessage().contains("saving user"));
        assertTrue(error.getMessage().startsWith("Database constraint violation while "));
        assertFalse(error.getMessage().contains(cause.getMessage()));
        assertNull(invalid.getId());
    }

    @Test
    void serviceStoresExactInjectionTextWithProductionValidation() throws SQLException {
        var friendships = new FriendshipDbRepository(new FriendshipValidator(users), url, username, password);
        var service = new UserService(users, new UserValidator(), friendships);
        User payload = track(new User("'); DROP TABLE users; --", "InjectionTest"));
        Long generatedId = service.addUser(payload);
        assertNotNull(generatedId);
        assertTrue(generatedId > 0);
        assertEquals(generatedId, payload.getId());
        assertUser(payload, users.findOne(payload.getId()).orElseThrow());
        // A direct read proves the table still exists and remains accessible.
        try (var c = connection();
             var ps = c.prepareStatement("SELECT id FROM users WHERE id = ?")) {
            ps.setLong(1, generatedId);
            try (var rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(generatedId.longValue(), rs.getLong("id"));
            }
        }
        User survivor = track(new User("Normal", "User"));
        service.addUser(survivor);
        assertUser(survivor, users.findOne(survivor.getId()).orElseThrow());
    }

    @Test
    void semicolonsAndLineBreaksRoundTripOnSaveAndUpdate() {
        String[][] names = {{"First;Other", "Last"}, {"First", "Last;Other"},
                {"First\nOther", "Last"}, {"First", "Last\nOther"},
                {"First\rOther", "Last"}, {"First", "Last\rOther"}};
        User original = createUser();
        for (String[] pair : names) {
            User candidate = track(new User(pair[0], pair[1]));
            users.save(candidate);
            assertUser(candidate, users.findOne(candidate.getId()).orElseThrow());
            User replacement = new User(original.getId(), pair[0], pair[1]);
            assertTrue(users.update(replacement).isEmpty());
            assertUser(replacement, users.findOne(original.getId()).orElseThrow());
        }
    }
}
