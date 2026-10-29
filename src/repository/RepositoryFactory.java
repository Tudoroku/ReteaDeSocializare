
package repository;

import domain.Friendship;
import domain.User;
import repository.db.FriendshipDbRepository;
import repository.db.UserDbRepository;
import repository.file.FriendshipFileRepository;
import repository.file.UserFileRepository;
import validation.Validator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Selects repository implementations from
 * {@code src/main/resources/config.properties} relative to the working directory.
 * Reads configuration once per factory. MEMORY ignores validators; FILE passes
 * them to repositories that load immediately; DATABASE creates placeholders and
 * does not open a connection. Configuration values used as paths or connection
 * settings are trimmed, except the database password.
 */
public class RepositoryFactory {

    private final Properties properties;
    private final RepositoryType type;

    /**
     * Loads properties and selects the case-sensitive {@code repository.type}.
     * Other settings are checked when creating the corresponding repository.
     * @throws RepositoryException if the configuration file cannot be read or closed
     * @throws IllegalArgumentException if the type is missing, blank or unknown,
     *         or the properties file contains a malformed Unicode escape
     */
    public RepositoryFactory() {
        properties = new Properties();

        try (InputStream input = Files.newInputStream(
                Path.of("src/main/resources/config.properties"))) {

            properties.load(input);
        }
        catch (IOException e) {
            throw new RepositoryException(
                    "Cannot load configuration file!", e
            );
        }

        String configuredType = properties.getProperty("repository.type");

        if (configuredType == null || configuredType.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing repository.type in config.properties!"
            );
        }

        try {
            type = RepositoryType.valueOf(configuredType.trim());
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown repository type: " + configuredType, e
            );
        }
    }
    /**
     * Creates a new user repository for the configured storage type.
     * FILE requires a nonblank {@code users.file}. DATABASE requires nonblank
     * {@code db.url} and {@code db.user}, and a present {@code db.password}
     * (an empty password is accepted).
     * @param validator the user validator; required only for FILE, ignored by
     *        MEMORY and merely stored by DATABASE
     * @return a new empty MEMORY repository, immediately loaded FILE repository,
     *         or unimplemented DATABASE repository
     * @throws IllegalArgumentException if a required setting is missing or blank,
     *         or the configured file path is invalid
     * @throws NullPointerException if FILE is selected and validator is null
     * @throws RepositoryException if a FILE repository cannot read its data
     */
    public UserRepository createUserRepository(Validator<User> validator) {
        switch (type) {
            case MEMORY:
                return new InMemoryUserRepository();

            case FILE:
                String filename = properties.getProperty("users.file");

                if (filename == null || filename.isBlank()) {
                    throw new IllegalArgumentException(
                            "Missing users.file in config.properties!"
                    );
                }

                return new UserFileRepository(filename.trim(), validator);

            case DATABASE:
                String url = properties.getProperty("db.url");
                String user = properties.getProperty("db.user");
                String password = properties.getProperty("db.password");

                if(url == null || url.isBlank())
                {
                    throw new IllegalArgumentException("Missing db.url in config.properties!");
                }
                if(user == null || user.isBlank())
                {
                    throw new IllegalArgumentException("Missing db.user in config.properties!");
                }
                if (password == null) {
                    throw new IllegalArgumentException(
                            "Missing db.password in config.properties!"
                    );
                }

                return new UserDbRepository(validator, url, user, password);

            default:
                throw new IllegalArgumentException(
                        "Unknown repository type: " + type
                );
        }
    }

    /**
     * Creates a new friendship repository for the configured storage type.
     * FILE requires a nonblank {@code friendships.file}. DATABASE requires nonblank
     * {@code db.url} and {@code db.user}, and a present {@code db.password}.
     * The caller supplies a validator linked to the appropriate users, if needed.
     * @param validator the friendship validator; required only for FILE, ignored
     *        by MEMORY and merely stored by DATABASE
     * @return a new empty MEMORY repository, immediately loaded FILE repository,
     *         or unimplemented DATABASE repository
     * @throws IllegalArgumentException if a required setting is missing or blank,
     *         or the configured file path is invalid
     * @throws NullPointerException if FILE is selected and validator is null
     * @throws RepositoryException if a FILE repository cannot read its data
     */
    public FriendshipRepository createFriendshipRepository(Validator<Friendship> validator)
    {
        switch (type) {
            case MEMORY:
                return new InMemoryFriendshipRepository();

            case FILE:
                String filename = properties.getProperty("friendships.file");

                if (filename == null || filename.isBlank()) {
                    throw new IllegalArgumentException(
                            "Missing friendships.file in config.properties!"
                    );
                }

                return new FriendshipFileRepository(filename.trim(), validator);

            case DATABASE:
                String url = properties.getProperty("db.url");
                String user = properties.getProperty("db.user");
                String password = properties.getProperty("db.password");

                if(url == null || url.isBlank())
                {
                    throw new IllegalArgumentException("Missing db.url in config.properties!");
                }
                if(user == null || user.isBlank())
                {
                    throw new IllegalArgumentException("Missing db.user in config.properties!");
                }
                if (password == null) {
                    throw new IllegalArgumentException(
                            "Missing db.password in config.properties!"
                    );
                }

                return new FriendshipDbRepository(validator, url, user, password);

            default:
                throw new IllegalArgumentException(
                        "Unknown repository type: " + type
                );
        }
    }
}
