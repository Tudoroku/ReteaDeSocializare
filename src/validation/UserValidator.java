package validation;

import domain.User;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

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

        if(entity.getName() == null || entity.getName().isBlank())
        {
            throw new IllegalArgumentException("Name cannot be empty!");
        }

        if(entity.getBirthday() == null)
        {
            throw new IllegalArgumentException("Birthday cannot be null!");
        }

        Clock clock = Clock.system(ZoneId.of("Europe/Bucharest"));
        LocalDate today = LocalDate.now(clock);

        if(entity.getBirthday().isAfter(today))
        {
            throw new IllegalArgumentException("Birthday cannot be in the future!");
        }

        if(entity.getBirthday().isBefore(today.minusYears(MAX_AGE_YEARS)))
        {
            throw new IllegalArgumentException("User cannot be over " + MAX_AGE_YEARS + " years old!");
        }
    }
}
