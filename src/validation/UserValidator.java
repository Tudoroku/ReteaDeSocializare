package validation;

import domain.User;


public class UserValidator implements Validator<User>{
    private static final int MAX_AGE_YEARS = 150;
    @Override
    public void validate(User entity) {
        if(entity == null)
        {
            throw new IllegalArgumentException("User cannot be null!");
        }
        if(entity.getId() == null || entity.getId() <= 0L)
        {
            throw new IllegalArgumentException("ID must be positive and cannot be null!");
        }

        if (entity.getFirstName() == null || entity.getFirstName().isBlank()) {
            throw new IllegalArgumentException("First name cannot be empty!");
        }

        if (entity.getLastName() == null || entity.getLastName().isBlank()) {
            throw new IllegalArgumentException("Last name cannot be empty!");
        }
        if (entity.getFirstName().contains(";") ||
                entity.getLastName().contains(";") ||
                entity.getFirstName().contains("\n") ||
                entity.getLastName().contains("\n") ||
                entity.getFirstName().contains("\r") ||
                entity.getLastName().contains("\r")) {

            throw new IllegalArgumentException(
                    "Names cannot contain semicolons or line breaks!"
            );
        }
    }
}
