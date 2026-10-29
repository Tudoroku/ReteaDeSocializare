
package repository.file;

import domain.Friendship;
import domain.FriendshipId;
import repository.AbstractFileRepository;
import repository.FriendshipRepository;
import validation.Validator;

import java.time.LocalDateTime;

/**
 * Persists friendships as {@code id1;id2;createdAt} lines, with normalized IDs and
 * an ISO local date-time. Uses the loading and rewrite behavior of
 * {@link AbstractFileRepository}; user-existence checks belong to the supplied validator.
 */
public class FriendshipFileRepository
        extends AbstractFileRepository<FriendshipId, Friendship>
        implements FriendshipRepository {

    /**
     * Configures the repository and immediately loads existing friendships.
     * Missing files produce an empty repository without creating a file. Invalid
     * lines are reported and skipped as described by {@link #loadFromFile()}.
     * @param filename the file path, relative to the working directory if relative
     * @param validator the non-null validator used during loading and mutations
     * @throws NullPointerException if filename or validator is null
     * @throws java.nio.file.InvalidPathException if filename is not a valid path
     * @throws repository.RepositoryException if loading encounters an I/O error
     */
    public FriendshipFileRepository(
            String filename,
            Validator<Friendship> validator) {
        super(filename, validator);
        loadFromFile();
    }

    /**
     * Parses exactly three semicolon-separated fields after trimming them.
     * Construction checks and normalizes the IDs; user existence is not checked here.
     * @param line the non-null source line
     * @return the friendship with the parsed local date-time
     * @throws IllegalArgumentException if the field count or numeric IDs are
     *         invalid, or the IDs are nonpositive or equal
     * @throws java.time.format.DateTimeParseException if the date-time is invalid
     * @throws NullPointerException if line is null
     */
    @Override
    protected Friendship extractEntity(String line) {
        String[] fields = line.split(";", -1);

        if (fields.length != 3) {
            throw new IllegalArgumentException(
                    "Invalid friendship format! Expected: id1;id2;createdAt"
            );
        }

        Long id1 = Long.parseLong(fields[0].trim());
        Long id2 = Long.parseLong(fields[1].trim());
        LocalDateTime createdAt =
                LocalDateTime.parse(fields[2].trim());

        return new Friendship(id1, id2, createdAt);
    }

    /**
     * Formats a friendship using its normalized IDs and local date-time.
     * @param entity the non-null friendship
     * @return the three semicolon-separated fields without a trailing newline
     * @throws NullPointerException if entity is null
     */
    @Override
    protected String createEntityAsString(Friendship entity) {
        return entity.getId().firstId() + ";" +
                entity.getId().secondId() + ";" +
                entity.getCreatedAt();
    }
}
