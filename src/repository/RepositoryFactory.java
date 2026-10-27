
package repository;

import domain.Friendship;
import domain.User;
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
    public UserRepository createUserRepository(
            Validator<User> validator) {
        // TODO
        return null;
    }

    public FriendshipRepository createFriendshipRepository(
            Validator<Friendship> validator) {
        // TODO
        return null;
    }
}
