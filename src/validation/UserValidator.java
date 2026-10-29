package validation;

import domain.User;


/**
 * Validates user IDs and names without modifying the user. Instances have no
 * configuration and can be created with the implicit public constructor.
 */
public class UserValidator implements Validator<User>{
    private static final int MAX_AGE_YEARS = 150;
    /**
     * {@inheritDoc}
     * Requires a positive ID and non-null, nonblank names. Names must not contain
     * semicolons, carriage returns or newlines; other surrounding whitespace is
     * not trimmed. Checks stop at the first violation.
     * @param entity the non-null user to check
     * @throws ValidationException if the user is null, its ID is null or
     *         nonpositive, or either name violates these rules
     */
    @Override
    public void validate(User entity) {
        if(entity == null)
        {
            throw new ValidationException("User cannot be null!");
        }
        if(entity.getId() == null || entity.getId() <= 0L)
        {
            throw new ValidationException("ID must be positive and cannot be null!");
        }

        if (entity.getFirstName() == null || entity.getFirstName().isBlank()) {
            throw new ValidationException("First name cannot be empty!");
        }

        if (entity.getLastName() == null || entity.getLastName().isBlank()) {
            throw new ValidationException("Last name cannot be empty!");
        }
        if (entity.getFirstName().contains(";") ||
                entity.getLastName().contains(";") ||
                entity.getFirstName().contains("\n") ||
                entity.getLastName().contains("\n") ||
                entity.getFirstName().contains("\r") ||
                entity.getLastName().contains("\r")) {

            throw new ValidationException(
                    "Names cannot contain semicolons or line breaks!"
            );
        }
    }
}
