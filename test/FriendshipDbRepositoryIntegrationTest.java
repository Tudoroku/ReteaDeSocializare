import domain.Friendship;
import domain.FriendshipId;
import domain.User;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import repository.RepositoryException;
import repository.db.FriendshipDbRepository;
import service.UserService;
import validation.FriendshipValidator;
import validation.UserValidator;
import validation.ValidationException;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Tag("jdbc")
class FriendshipDbRepositoryIntegrationTest extends JdbcTestSupport {
    // PostgreSQL timestamps have microsecond precision; avoid unstable nanosecond round trips.
    private static final LocalDateTime DATE = LocalDateTime.of(2025, 2, 3, 10, 20, 30, 123456000);

    private FriendshipDbRepository friendships() {
        return new FriendshipDbRepository(new FriendshipValidator(users), url, username, password);
    }

    private Friendship pair() {
        return new Friendship(createUser().getId(), createUser().getId(), DATE);
    }

    private static void assertFriendship(Friendship expected, Friendship actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getCreatedAt(), actual.getCreatedAt());
    }

    @Test
    void saveAndFindByCompositeIdIncludingReversedIds() {
        var repo = friendships();
        Friendship original = pair();
        repo.save(original);
        assertFriendship(original, repo.findOne(original.getId()).orElseThrow());
        assertFriendship(original, repo.findOne(new FriendshipId(
                original.getId().secondId(), original.getId().firstId())).orElseThrow());
    }

    @Test
    void findAllContainsPersistedFriendships() {
        var repo = friendships();
        Friendship first = pair();
        Friendship second = pair();
        repo.save(first);
        repo.save(second);
        var found = repo.findAll(f -> f.getId().equals(first.getId()) || f.getId().equals(second.getId()));
        assertEquals(2, found.size());
        assertFriendship(first, found.stream().filter(f -> f.getId().equals(first.getId())).findFirst().orElseThrow());
        assertFriendship(second, found.stream().filter(f -> f.getId().equals(second.getId())).findFirst().orElseThrow());
    }

    @Test
    void duplicateAndReversedDuplicateAreRejected() {
        var repo = friendships();
        Friendship first = pair();
        repo.save(first);
        assertThrows(IllegalArgumentException.class, () -> repo.save(first));
        assertThrows(IllegalArgumentException.class, () -> repo.save(new Friendship(
                first.getId().secondId(), first.getId().firstId(), DATE.plusDays(1))));
        assertFriendship(first, repo.findOne(first.getId()).orElseThrow());
    }

    @Test
    void nonexistentUsersFailApplicationValidation() {
        var repo = friendships();
        Friendship missing = new Friendship(createUser().getId(), missingUserId(), DATE);
        assertThrows(ValidationException.class, () -> repo.save(missing));
        assertTrue(repo.findOne(missing.getId()).isEmpty());
    }

    @Test
    void foreignKeysRejectEitherMissingEndpointAndPreserveSqlCause() {
        // Bypass only existence validation to reach actual database FK enforcement (no mocks).
        var jdbcOnly = new FriendshipDbRepository(f -> {}, url, username, password);
        Long lower = missingUserId();
        User existing = createUser();
        Long upper = missingUserId();
        for (Long missing : new Long[]{lower, upper}) {
            Friendship invalid = new Friendship(existing.getId(), missing, DATE);
            RepositoryException error = assertThrows(RepositoryException.class, () -> jdbcOnly.save(invalid));
            SQLException cause = assertInstanceOf(SQLException.class, error.getCause());
            assertEquals("23503", cause.getSQLState());
            assertTrue(error.getMessage().contains("saving friendship"));
            assertTrue(error.getMessage().startsWith("Database constraint violation while "));
            assertFalse(error.getMessage().contains(cause.getMessage()));
            assertTrue(jdbcOnly.findOne(invalid.getId()).isEmpty());
        }
    }

    @Test
    void updateChangesDateUsingReversedIdsAndPreservesOtherFriendship() {
        var repo = friendships();
        Friendship original = pair();
        Friendship other = pair();
        repo.save(original);
        repo.save(other);
        Friendship replacement = new Friendship(original.getId().secondId(), original.getId().firstId(), DATE.plusDays(2));
        assertTrue(repo.update(replacement).isEmpty());
        assertFriendship(replacement, repo.findOne(original.getId()).orElseThrow());
        assertFriendship(other, repo.findOne(other.getId()).orElseThrow());
    }

    @Test
    void missingPairWithExistingUsersFollowsOptionalContract() {
        var repo = friendships();
        Friendship missing = pair();
        assertTrue(repo.findOne(missing.getId()).isEmpty());
        assertTrue(repo.delete(missing.getId()).isEmpty());
        assertSame(missing, repo.update(missing).orElseThrow());
        assertTrue(repo.findOne(missing.getId()).isEmpty());
    }

    @Test
    void deleteUsingReversedIdReturnsFullFriendshipAndThenEmpty() {
        var repo = friendships();
        Friendship original = pair();
        repo.save(original);
        var reversed = new FriendshipId(original.getId().secondId(), original.getId().firstId());
        assertFriendship(original, repo.delete(reversed).orElseThrow());
        assertTrue(repo.findOne(original.getId()).isEmpty());
        assertTrue(repo.delete(reversed).isEmpty());
    }

    @Test
    void deletingUserDirectlyCascadesBothEndpointsAndPreservesUnrelatedRows() {
        var repo = friendships();
        User first = createUser();
        User middle = createUser();
        User last = createUser();
        Friendship left = new Friendship(first.getId(), middle.getId(), DATE);
        Friendship right = new Friendship(middle.getId(), last.getId(), DATE);
        Friendship unrelated = new Friendship(first.getId(), last.getId(), DATE);
        repo.save(left);
        repo.save(right);
        repo.save(unrelated);
        assertUser(middle, users.delete(middle.getId()).orElseThrow());
        assertTrue(repo.findOne(left.getId()).isEmpty());
        assertTrue(repo.findOne(right.getId()).isEmpty());
        assertFriendship(unrelated, repo.findOne(unrelated.getId()).orElseThrow());
        assertUser(first, users.findOne(first.getId()).orElseThrow());
        assertUser(last, users.findOne(last.getId()).orElseThrow());
    }

    @Test
    void serviceDeletesUserAndAssociatedFriendships() {
        var repo = friendships();
        Friendship friendship = pair();
        repo.save(friendship);
        var service = new UserService(users, new UserValidator(), repo);
        service.deleteUser(friendship.getId().firstId());
        assertTrue(users.findOne(friendship.getId().firstId()).isEmpty());
        assertTrue(repo.findOne(friendship.getId()).isEmpty());
        assertTrue(users.findOne(friendship.getId().secondId()).isPresent());
    }
}
