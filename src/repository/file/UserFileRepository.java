
package repository.file;

import domain.User;
import repository.AbstractFileRepository;
import repository.UserRepository;
import validation.Validator;

public class UserFileRepository
        extends AbstractFileRepository<Long, User>
        implements UserRepository {

    public UserFileRepository(String filename, Validator<User> validator) {
        super(filename, validator);
        loadFromFile();
    }

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

    @Override
    protected String createEntityAsString(User entity) {
        return entity.getId() + ";" +
                entity.getFirstName() + ";" +
                entity.getLastName();
    }
}
