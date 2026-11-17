package repository.db;

import domain.User;
import repository.RepositoryException;
import repository.UserRepository;
import validation.Validator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Persists users in PostgreSQL using JDBC prepared statements and a connection
 * per operation. PostgreSQL generates IDs, which save assigns to the supplied user.
 */
public class UserDbRepository implements UserRepository {
    private final Validator<User> validator;
    private final String url;
    private final String user;
    private final String password;

    /**
     * Reports that PostgreSQL generates user identifiers.
     * @return true
     */
    @Override
    public boolean generatesIds() {
        return true;
    }
    /**
     * Stores non-null dependencies without opening a connection.
     * @param validator the validator used before save and update
     * @param url the JDBC connection URL
     * @param user the database username
     * @param password the database password
     * @throws NullPointerException if any argument is null
     */
    public UserDbRepository(Validator<User> validator, String url, String user, String password) {
        this.validator = Objects.requireNonNull(validator);
        this.url = Objects.requireNonNull(url);
        this.user = Objects.requireNonNull(user);
        this.password = Objects.requireNonNull(password);
    }

    /**
     * Finds a user by identifier.
     * @param id the non-null identifier
     * @return the matching user, or an empty optional if absent
     * @throws IllegalArgumentException if id is null
     * @throws RepositoryException if database access fails
     */
    @Override
    public Optional<User> findOne(Long id) {
        if(id == null)
        {
            throw new IllegalArgumentException("Id cannot be null!");
        }
        String sql = "SELECT id, first_name, last_name FROM users WHERE id = ?";
        try (Connection c = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = c.prepareStatement(sql))
        {


                ps.setLong(1, id);
                try(ResultSet rs = ps.executeQuery())
                {
                    if (rs.next()) {
                        return Optional.of(new User(
                                rs.getLong("id"),
                                rs.getString("first_name"),
                                rs.getString("last_name")
                        ));
                    }
                }

        }
        catch (SQLException e)
        {
            throw JdbcErrors.failure("finding user", e);
        }
        return Optional.empty();
    }

    /**
     * Retrieves users ordered by identifier.
     * @return the stored users, or an empty iterable
     * @throws RepositoryException if database access fails
     */

    @Override
    public Iterable<User> findAll() {
        List<User> users = new ArrayList<>();

        String sql = "SELECT id, first_name, last_name FROM users ORDER BY id";

        try (Connection c = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                User found = new User(
                        rs.getLong("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name")
                );

                users.add(found);
            }

        } catch (SQLException e) {
            throw JdbcErrors.failure("retrieving users", e);
        }

        return users;
    }


    /**
     * Validates and inserts a user, assigning the PostgreSQL-generated ID.
     * @param entity the user to insert, with a null ID
     * @throws IllegalArgumentException if entity is null or already has an ID
     * @throws validation.ValidationException if validation fails
     * @throws RepositoryException if database access fails or no ID is returned
     */
    @Override
    public void save(User entity) {
        if (entity == null) {
            throw new IllegalArgumentException("User cannot be null!");
        }

        if (entity.getId() != null) {
            throw new IllegalArgumentException(
                    "User ID must be null! PostgreSQL generates it."
            );
        }

        validator.validate(entity);

        String sql = """
            INSERT INTO users (first_name, last_name)
            VALUES (?, ?)
            RETURNING id
            """;

        try (Connection c = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, entity.getFirstName());
            ps.setString(2, entity.getLastName());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    entity.setId(rs.getLong("id"));
                } else {
                    throw new SQLException("No generated ID returned!");
                }
            }

        } catch (SQLException e) {
            throw JdbcErrors.failure("saving user", e);
        }
    }


    /**
     * Deletes a user; database foreign keys cascade to associated friendships.
     * @param id the non-null identifier to delete
     * @return the deleted user, or an empty optional if absent
     * @throws IllegalArgumentException if id is null
     * @throws RepositoryException if database access fails
     */

    @Override
    public Optional<User> delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null!");
        }

        String sql = """
            DELETE FROM users
            WHERE id = ?
            RETURNING id, first_name, last_name
            """;

        try (Connection c = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getLong("id"),
                            rs.getString("first_name"),
                            rs.getString("last_name")
                    ));
                }
            }

        } catch (SQLException e) {
            throw JdbcErrors.failure("deleting user", e);
        }

        return Optional.empty();
    }


    /**
     * Validates and updates the names of an existing user.
     * @param entity the replacement user with a non-null ID
     * @return an empty optional on success, or the supplied entity if absent
     * @throws IllegalArgumentException if entity or its ID is null
     * @throws validation.ValidationException if validation fails
     * @throws RepositoryException if database access fails
     */

    @Override
    public Optional<User> update(User entity) {
        if (entity == null || entity.getId() == null) {
            throw new IllegalArgumentException("User or ID cannot be null!");
        }

        validator.validate(entity);

        String sql = """
            UPDATE users
            SET first_name = ?, last_name = ?
            WHERE id = ?
            """;

        try (Connection c = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, entity.getFirstName());
            ps.setString(2, entity.getLastName());
            ps.setLong(3, entity.getId());

            int rows = ps.executeUpdate();

            return rows == 0 ? Optional.of(entity) : Optional.empty();

        } catch (SQLException e) {
            throw JdbcErrors.failure("updating user", e);
        }
    }

}
