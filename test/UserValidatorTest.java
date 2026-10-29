import domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import validation.UserValidator;
import validation.ValidationException;
import validation.Validator;
import static org.junit.jupiter.api.Assertions.*;


public class UserValidatorTest {
    private final Validator<User> validator = new UserValidator();

    @Test
    void testValidUserValidation()
    {
            User user = new User(
                    1L,
                    "Gheorghe",
                    "Popescu"
            );
        assertDoesNotThrow(new Executable()
        {
            @Override
            public void execute()
            {
                validator.validate(user);
            }
        });
        assertEquals("Gheorghe", user.getFirstName());
        assertEquals("Popescu", user.getLastName());
    }

    @Test
    void testNullUserValidation()
    {
        User user = null;
        assertThrows(ValidationException.class, new Executable()
        {
            @Override
            public void execute()
            {
                validator.validate(user);
            }
        });
    }

    @Test
    void testIdValidation()
    {
        //Test null id
        User user1 = new User(
                null,
                "name",
                "Popescu"
        );
        assertThrows(ValidationException.class, new Executable()
        {
            @Override
            public void execute()
            {
                validator.validate(user1);
            }
        });

        //Test id == 0
        User user2 = new User(
                0L,
                "name",
                "Popescu"
        );
        assertThrows(ValidationException.class, new Executable()
        {
            @Override
            public void execute()
            {
                validator.validate(user2);
            }
        });

        //Test negative id
        User user3 = new User(
                -2L,
                "name",
                "Popescu"
        );
        assertThrows(ValidationException.class, new Executable()
        {
            @Override
            public void execute()
            {
                validator.validate(user3);
            }
        });
    }

    @Test
    void testFirstNameValidation()
    {
        //Test null first name
        User user1 = new User(
                1L,
                null,
                "Popescu"
        );
        assertThrows(ValidationException.class, new Executable()
        {
            @Override
            public void execute()
            {
                validator.validate(user1);
            }
        });

        //Test empty first name
        User user2 = new User(
                1L,
                "",
                "Popescu"
        );
        assertThrows(ValidationException.class, new Executable()
        {
            @Override
            public void execute()
            {
                validator.validate(user2);
            }
        });

        //Test blank first name (with spaces)
        User user3 = new User(
                1L,
                "     ",
                "Popescu"
        );
        assertThrows(ValidationException.class, new Executable()
        {
            @Override
            public void execute()
            {
                validator.validate(user3);
            }
        });
    }

    @Test
    void testLastNameValidation()
    {
        //Test null last name
        User user1 = new User(
                1L,
                "Sebastian",
                null
        );
        assertThrows(ValidationException.class, new Executable()
        {
            @Override
            public void execute()
            {
                validator.validate(user1);
            }
        });

        //Test empty last name
        User user2 = new User(
                1L,
                "Sebastian",
                ""
        );
        assertThrows(ValidationException.class, new Executable()
        {
            @Override
            public void execute()
            {
                validator.validate(user2);
            }
        });

        //Test blank last name (with spaces, tabs and newlines)
        User user3 = new User(
                1L,
                "Sebastian",
                " \t\n "
        );
        assertThrows(ValidationException.class, new Executable()
        {
            @Override
            public void execute()
            {
                validator.validate(user3);
            }
        });

        //Test valid names containing spaces, hyphens and apostrophes
        User user4 = new User(
                1L,
                "Ana Maria",
                "Popescu-O'Neil"
        );
        assertDoesNotThrow(new Executable()
        {
            @Override
            public void execute()
            {
                validator.validate(user4);
            }
        });


    }

}
