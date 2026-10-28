
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

public class RepositoryFactory {

    private final Properties properties;
    private final RepositoryType type;

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
