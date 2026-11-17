package repository.db;

import domain.User;
import repository.RepositoryException;
import repository.UserRepository;
import validation.Validator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Placeholder for user database persistence. Stores constructor arguments only;
 * no connection, validation or CRUD operation is implemented.
 */
public class UserDbRepository implements UserRepository {
    private final Validator<User> validator;
    private final String url;
    private final String user;
    private final String password;

    @Override
    public boolean generatesIds() {
        return true;
    }
    /**
     * Stores database settings without checking them or opening a connection.
     * @param validator the validator reserved for future use; may be null
     * @param url the database URL reserved for future use; may be null
     * @param user the database username reserved for future use; may be null
     * @param password the database password reserved for future use; may be null
     */
    public UserDbRepository(Validator<User> validator, String url, String user, String password) {
        this.validator = validator;
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Rejects lookup because database persistence is not implemented.
     * @param id the ignored identifier, including null
     * @return no result; this method always throws
     * @throws UnsupportedOperationException always
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
            throw new RepositoryException("Error finding user with ID " + id, e);
        }
        return Optional.empty();
    }

    /**
     * Rejects retrieval because database persistence is not implemented.
     * @return no iterable; this method always throws
     * @throws UnsupportedOperationException always
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
            throw new RepositoryException("Error retrieving users", e);
        }

        return users;
    }


    /**
     * Rejects insertion before performing any validation or storage.
     * @param entity the ignored entity, including null
     * @throws UnsupportedOperationException always
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
            throw new RepositoryException("Error saving user", e);
        }
    }


    /**
     * Rejects deletion without examining the identifier.
     * @param aLong the ignored identifier, including null
     * @throws UnsupportedOperationException always
     */
    @Override
    public void delete(Long aLong) {
        throw new UnsupportedOperationException(
                "Database persistence is not implemented yet"
        );
    }

    /**
     * Rejects replacement before performing any validation or storage.
     * @param entity the ignored replacement, including null
     * @throws UnsupportedOperationException always
     */
    @Override
    public void update(User entity) {
        throw new UnsupportedOperationException(
                "Database persistence is not implemented yet"
        );
    }
}
