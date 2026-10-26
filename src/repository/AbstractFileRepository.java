
package repository;

import domain.HasId;
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

public abstract class AbstractFileRepository<ID, E extends HasId<ID>>
        extends InMemoryRepository<ID, E> {

    protected final Path filePath;
    protected final Validator<E> validator;

    protected AbstractFileRepository(String filename, Validator<E> validator) {
        this.filePath = Path.of(filename);
        this.validator = Objects.requireNonNull(
                validator, "Validator cannot be null!"
        );
    }

    protected abstract E extractEntity(String line);

    protected abstract String createEntityAsString(E entity);


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
                catch (IllegalArgumentException | DateTimeParseException e) {
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

    @Override
    public void save(E entity) {
        validator.validate(entity);
        super.save(entity);
        writeToFile();
    }

    @Override
    public void delete(ID id) {
        super.delete(id);
        writeToFile();
    }

    @Override
    public void update(E entity) {
        validator.validate(entity);
        super.update(entity);
        writeToFile();
    }
}
