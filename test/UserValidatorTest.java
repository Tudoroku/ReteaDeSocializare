import domain.User;
import org.junit.jupiter.api.Test;
import validation.UserValidator;
import validation.Validator;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

public class UserValidatorTest {
    private final Validator<User> validator = new UserValidator();

    @Test
    void testValidUserValidation()
    {
            User user = new User(
                    1L,
                    "Gheorghe",
                    LocalDate.of(2002, 3, 20)
            );
        assertDoesNotThrow(() -> {
            validator.validate(user);
        });
    }

    @Test
    void testNullUserValidation()
    {
        User user = null;
        assertThrows(IllegalArgumentException.class, () ->
        {
            validator.validate(user);
        });
    }

    @Test
    void testIdValidation()
    {
        //Test null id
        User user1 = new User(
                null,
                "name",
                LocalDate.of(2000, 1, 1)
        );
        assertThrows(IllegalArgumentException.class, () ->
        {
            validator.validate(user1);
        });

        //Test id == 0
        User user2 = new User(
                0L,
                "name",
                LocalDate.of(2000, 1, 1)
        );
        assertThrows(IllegalArgumentException.class, () ->
        {
            validator.validate(user2);
        });

        //Test negative id
        User user3 = new User(
                -2L,
                "name",
                LocalDate.of(2000, 1, 1)
        );
        assertThrows(IllegalArgumentException.class, () ->
        {
            validator.validate(user3);
        });
    }

    @Test
    void testNameValidation()
    {
        //Test null name
        User user1 = new User(
                1L,
                null,
                LocalDate.of(2000, 1, 1)
        );
        assertThrows(IllegalArgumentException.class, () ->
        {
            validator.validate(user1);
        });

        //Test empty name
        User user2 = new User(
                1L,
                "",
                LocalDate.of(2000, 1, 1)
        );
        assertThrows(IllegalArgumentException.class, () ->
        {
            validator.validate(user2);
        });

        //Test blank name (with spaces)
        User user3 = new User(
                1L,
                "     ",
                LocalDate.of(2000, 1, 1)
        );
        assertThrows(IllegalArgumentException.class, () ->
        {
            validator.validate(user3);
        });
    }

    @Test
    void testBirthdateValidation()
    {
        //Test null birthdate
        User user1 = new User(
                1L,
                "Sebastian",
                null
        );
        assertThrows(IllegalArgumentException.class, () ->
        {
            validator.validate(user1);
        });

        //Test future birthdate
        User user2 = new User(
                1L,
                "Sebastian",
                LocalDate.of(LocalDate.now().getYear() + 1, 1, 1)
        );
        assertThrows(IllegalArgumentException.class, () ->
        {
            validator.validate(user2);
        });

        User user3 = new User(
                1L,
                "Sebastian",
                LocalDate.of(LocalDate.now().getYear() - 150,
                        LocalDate.now().getMonth(),
                        LocalDate.now().getDayOfMonth())
        );
        assertDoesNotThrow( () ->
        {
            validator.validate(user3);
        });

        User user4 = new User(
                1L,
                "Sebastian",
                LocalDate.of(LocalDate.now().getYear() - 151, 1, 1)
        );
        assertThrows(IllegalArgumentException.class, () ->
        {
           validator.validate(user4);
        });


    }

}
