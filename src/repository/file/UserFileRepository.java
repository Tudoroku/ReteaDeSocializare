
package repository.file;

import domain.User;
import repository.AbstractFileRepository;
import repository.UserRepository;
import validation.Validator;

/**
 * Persists users as {@code id;firstName;lastName} lines using the loading and
 * rewrite behavior of {@link AbstractFileRepository}. Loaded fields are trimmed;
 * saved fields are emitted as supplied. Suitability of names for this format is
 * the supplied validator's responsibility.
 */
public class UserFileRepository
        extends AbstractFileRepository<Long, User>
        implements UserRepository {

    /**
     * Configures the repository and immediately loads existing users.
     * Missing files produce an empty repository without creating a file. Invalid
     * lines are reported and skipped as described by {@link #loadFromFile()}.
     * @param filename the file path, relative to the working directory if relative
     * @param validator the non-null validator used during loading and mutations
     * @throws NullPointerException if filename or validator is null
     * @throws java.nio.file.InvalidPathException if filename is not a valid path
     * @throws repository.RepositoryException if loading encounters an I/O error
     */
    public UserFileRepository(String filename, Validator<User> validator) {
        super(filename, validator);
        loadFromFile();
    }

    /**
     * Parses exactly three semicolon-separated fields, trimming each field.
     * Does not validate the resulting user.
     * @param line the non-null source line
     * @return a user containing the parsed ID and trimmed names
     * @throws IllegalArgumentException if the field count is not three or the ID
     *         cannot be parsed as a {@code long}
     * @throws NullPointerException if line is null
     */
    @Override
    protected User extractEntity(String line) {
        String[] fields = line.split(";", -1);

        if (fields.length != 3) {
            throw new IllegalArgumentException(
                    "Invalid user format! Expected: id;firstName;lastName"
            );
        }

        Long id = Long.parseLong(fields[0].trim());
        String firstName = fields[1].trim();
        String lastName = fields[2].trim();

        return new User(id, firstName, lastName);
    }

    /**
     * Formats a user without escaping or trimming its fields.
     * @param entity the non-null user
     * @return the ID and names separated by semicolons, without a trailing newline
     * @throws NullPointerException if entity is null
     */
    @Override
    protected String createEntityAsString(User entity) {
        return entity.getId() + ";" +
                entity.getFirstName() + ";" +
                entity.getLastName();
    }
}
