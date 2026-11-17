
package repository.db;

import domain.Friendship;
import domain.FriendshipId;
import repository.FriendshipRepository;
import repository.RepositoryException;
import validation.Validator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * PostgreSQL implementation of FriendshipRepository.
 * Uses JDBC and prepared statements for persistence.
 */
public class FriendshipDbRepository implements FriendshipRepository {

    private final Validator<Friendship> validator;
    private final String url;
    private final String user;
    private final String password;

    /**
     * Creates a repository using the supplied database configuration.
     *
     * @param validator validator for friendships
     * @param url JDBC connection URL
     * @param user database username
     * @param password database password
     * @throws NullPointerException if any argument is null
     */
    public FriendshipDbRepository(
            Validator<Friendship> validator,
            String url,
            String user,
            String password) {

        this.validator = Objects.requireNonNull(validator);
        this.url = Objects.requireNonNull(url);
        this.user = Objects.requireNonNull(user);
        this.password = Objects.requireNonNull(password);
    }

    /**
     * Converts a database row into a Friendship.
     *
     * @param rs result set positioned on a valid row
     * @return the reconstructed friendship
     * @throws SQLException if reading the row fails
     */
    private Friendship extractFriendship(ResultSet rs)
            throws SQLException {

        return new Friendship(
                rs.getLong("id1"),
                rs.getLong("id2"),
                rs.getTimestamp("friends_from").toLocalDateTime()
        );
    }

    /**
     * Finds a friendship by its normalized composite ID.
     *
     * @param friendshipId identifier to search for
     * @return the friendship, or Optional.empty() if absent
     * @throws IllegalArgumentException if friendshipId is null
     * @throws RepositoryException if database access fails
     */
    @Override
    public Optional<Friendship> findOne(FriendshipId friendshipId) {
        if (friendshipId == null) {
            throw new IllegalArgumentException("Friendship ID cannot be null!");
        }

        String sql = """
                SELECT id1, id2, friends_from
                FROM friendships
                WHERE id1 = ? AND id2 = ?
                """;

        try (Connection c = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, friendshipId.firstId());
            ps.setLong(2, friendshipId.secondId());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(extractFriendship(rs));
                }
            }

        } catch (SQLException e) {
            throw JdbcErrors.failure("finding friendship", e);
        }

        return Optional.empty();
    }

    /**
     * Retrieves all friendships from the database.
     *
     * @return an iterable containing all friendships
     * @throws RepositoryException if database access fails
     */
    @Override
    public Iterable<Friendship> findAll() {
        List<Friendship> friendships = new ArrayList<>();

        String sql = """
                SELECT id1, id2, friends_from
                FROM friendships
                ORDER BY id1, id2
                """;

        try (Connection c = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                friendships.add(extractFriendship(rs));
            }

        } catch (SQLException e) {
            throw JdbcErrors.failure("retrieving friendships", e);
        }

        return friendships;
    }

    /**
     * Validates and saves a new friendship.
     *
     * @param entity friendship to insert
     * @throws IllegalArgumentException if entity is null or already exists
     * @throws validation.ValidationException if validation fails
     * @throws RepositoryException if database access fails
     */
    @Override
    public void save(Friendship entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Friendship cannot be null!");
        }

        validator.validate(entity);

        if (findOne(entity.getId()).isPresent()) {
            throw new IllegalArgumentException("Friendship already exists!");
        }

        String sql = """
                INSERT INTO friendships (id1, id2, friends_from)
                VALUES (?, ?, ?)
                """;

        try (Connection c = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, entity.getId().firstId());
            ps.setLong(2, entity.getId().secondId());
            ps.setTimestamp(3, Timestamp.valueOf(entity.getCreatedAt()));

            ps.executeUpdate();

        } catch (SQLException e) {
            throw JdbcErrors.failure("saving friendship", e);
        }
    }

    /**
     * Updates the creation date of an existing friendship.
     *
     * @param entity friendship containing the updated values
     * @return Optional.empty() on success, or the supplied entity if absent
     * @throws IllegalArgumentException if entity is null
     * @throws validation.ValidationException if validation fails
     * @throws RepositoryException if database access fails
     */
    @Override
    public Optional<Friendship> update(Friendship entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Friendship cannot be null!");
        }

        validator.validate(entity);

        String sql = """
                UPDATE friendships
                SET friends_from = ?
                WHERE id1 = ? AND id2 = ?
                """;

        try (Connection c = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(entity.getCreatedAt()));
            ps.setLong(2, entity.getId().firstId());
            ps.setLong(3, entity.getId().secondId());

            int rows = ps.executeUpdate();

            return rows == 0
                    ? Optional.of(entity)
                    : Optional.empty();

        } catch (SQLException e) {
            throw JdbcErrors.failure("updating friendship", e);
        }
    }

    /**
     * Deletes a friendship and returns its previous value.
     *
     * @param friendshipId identifier to delete
     * @return the deleted friendship, or Optional.empty() if absent
     * @throws IllegalArgumentException if friendshipId is null
     * @throws RepositoryException if database access fails
     */
    @Override
    public Optional<Friendship> delete(FriendshipId friendshipId) {
        if (friendshipId == null) {
            throw new IllegalArgumentException("Friendship ID cannot be null!");
        }

        String sql = """
                DELETE FROM friendships
                WHERE id1 = ? AND id2 = ?
                RETURNING id1, id2, friends_from
                """;

        try (Connection c = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, friendshipId.firstId());
            ps.setLong(2, friendshipId.secondId());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(extractFriendship(rs));
                }
            }

        } catch (SQLException e) {
            throw JdbcErrors.failure("deleting friendship", e);
        }

        return Optional.empty();
    }
}
