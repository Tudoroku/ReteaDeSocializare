
package repository;

import domain.HasId;
import validation.ValidationException;
import validation.Validator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;

/**
 * Adds validated UTF-8 text persistence to an in-memory repository.
 * Subclasses define the line format and invoke {@link #loadFromFile()} after
 * initialization. Reads use the memory snapshot rather than rereading the file.
 * Successful mutations rewrite the whole file in unspecified entity order.
 * A write can truncate or partially replace the file before failing; there is
 * no disk rollback or atomic replacement.
 * @param <ID> the identifier type
 * @param <E> the entity type
 */
public abstract class AbstractFileRepository<ID, E extends HasId<ID>>
        extends InMemoryRepository<ID, E> {

    /** The configured path used for initial loading and subsequent rewrites. */
    protected final Path filePath;
    /** The validator applied before loading, saving or updating an entity. */
    protected final Validator<E> validator;

    /**
     * Configures persistence without loading or creating a file.
     * @param filename the path string, resolved relative to the working directory if relative
     * @param validator the non-null entity validator
     * @throws NullPointerException if either argument is null
     * @throws java.nio.file.InvalidPathException if the filename cannot form a path
     */
    protected AbstractFileRepository(String filename, Validator<E> validator) {
        this.filePath = Path.of(filename);
        this.validator = Objects.requireNonNull(
                validator, "Validator cannot be null!"
        );
    }

    /**
     * Parses one nonblank line; entity validation is performed by the loader afterwards.
     * @param line the non-null line to parse
     * @return the parsed entity
     * @throws IllegalArgumentException if the format or entity arguments are invalid
     * @throws DateTimeParseException if a date field cannot be parsed
     */
    protected abstract E extractEntity(String line);

    /**
     * Formats a stored entity as one line without a terminating line separator.
     * @param entity the non-null stored entity
     * @return the entity's persisted representation
     */
    protected abstract String createEntityAsString(E entity);


    /**
     * Adds entities from the configured UTF-8 file to the current memory contents.
     * A missing file leaves memory unchanged and is not created. Blank lines are
     * ignored. Parsing, validation and duplicate-ID failures of type
     * {@link IllegalArgumentException}, {@link ValidationException} or
     * {@link DateTimeParseException} are reported to standard error and skipped;
     * subsequent lines are still read. The file is never rewritten during loading.
     * Other unchecked failures propagate. An I/O failure may leave previously
     * loaded entities in memory.
     * @throws RepositoryException if opening, reading or closing the file fails
     */
    protected final void loadFromFile() {
        if (!Files.exists(filePath)) {
            return;
        }

        try (BufferedReader br = Files.newBufferedReader(
                filePath, StandardCharsets.UTF_8)) {

            String currentLine;
            int lineNumber = 0;

            while ((currentLine = br.readLine()) != null) {
                lineNumber++;

                if (currentLine.isBlank()) {
                    continue;
                }

                try {
                    E entity = extractEntity(currentLine);
                    validator.validate(entity);
                    super.save(entity);
                }
                catch (IllegalArgumentException | ValidationException | DateTimeParseException e) {
                    System.err.println(
                            "Invalid line " + lineNumber +
                                    " in " + filePath + ": " + e.getMessage()
                    );
                }
            }
        }
        catch (IOException e) {
            throw new RepositoryException(
                    "Error reading file: " + filePath, e
            );
        }
    }



    private void writeToFile() {
        try {
            Path parent = filePath.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (BufferedWriter bw = Files.newBufferedWriter(
                    filePath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {

                for (E entity : super.findAll()) {
                    bw.write(createEntityAsString(entity));
                    bw.newLine();
                }
            }
        }
        catch (IOException e) {
            throw new RepositoryException(
                    "Error writing to file: " + filePath, e
            );
        }
    }

    /**
     * Validates the entity, inserts it in memory, then rewrites the file.
     * Validation and duplicate checks occur before any write. If writing throws
     * {@link RepositoryException}, the inserted mapping is removed from memory;
     * file contents and created directories are not restored. Other unchecked
     * failures are propagated without this memory restoration.
     * @param entity the entity accepted by the validator, with a non-null unique ID
     * @throws ValidationException if validation rejects the entity
     * @throws IllegalArgumentException if the entity or ID is null after validation,
     *         or the identifier is already stored
     * @throws RepositoryException if rewriting the file fails
     */
    @Override
    public void save(E entity) {
        validator.validate(entity);
        super.save(entity);
        try {
            writeToFile();
        }
        catch (RepositoryException e)
        {
            super.delete(entity.getId());
            throw e;
        }
    }

    /**
     * Removes a stored entity from memory and rewrites the file without revalidating it.
     * If writing throws {@link RepositoryException}, the original object reference
     * is reinserted in memory. File contents are not restored; other unchecked
     * failures do not trigger this memory restoration.
     * @param id the existing identifier to remove
     * @throws IllegalArgumentException if the ID is null or not stored
     * @throws RepositoryException if rewriting the file fails
     */
    @Override
    public void delete(ID id) {
        Optional<E> entity = super.findOne(id);
        if(entity.isEmpty())
        {
            throw new IllegalArgumentException("Inexistent Id");
        }
        super.delete(id);
        try {
            writeToFile();
        }
        catch (RepositoryException e)
        {
            super.save(entity.get());
            throw e;
        }
    }

    /**
     * Validates a replacement before checking its existence, then updates memory
     * and rewrites the file. If writing throws {@link RepositoryException}, the
     * original object reference is restored in memory. File contents are not
     * restored; other unchecked failures do not trigger this memory restoration.
     * @param entity the replacement accepted by the validator, with an existing ID
     * @throws ValidationException if validation rejects the replacement
     * @throws IllegalArgumentException if its ID is absent, or memory argument checks fail
     * @throws NullPointerException if the validator accepts a null replacement
     * @throws RepositoryException if rewriting the file fails
     */
    @Override
    public void update(E entity) {
        validator.validate(entity);
        Optional<E> oldEntity = super.findOne(entity.getId());
        if(oldEntity.isEmpty())
        {
            throw new IllegalArgumentException("Invalid entity");
        }
        super.update(entity);
        try
        {
            writeToFile();
        }
        catch (RepositoryException e)
        {
            super.update(oldEntity.get());
            throw e;
        }
    }
}
