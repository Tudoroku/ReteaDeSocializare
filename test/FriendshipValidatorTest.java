import domain.Friendship;
import domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import repository.InMemoryUserRepository;
import validation.FriendshipValidator;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class FriendshipValidatorTest {

    @Test
    void testValidFriendshipValidation()
    {
        var userRepo = new InMemoryUserRepository();
        User first = new User(1L, "Ana", "Popescu");
        User second = new User(2L, "Sebi", "Ionescu");
        userRepo.save(first);
        userRepo.save(second);
        var validator = new FriendshipValidator(userRepo);
        LocalDateTime date = LocalDateTime.of(2024, 1, 1, 10, 0);
        Friendship friendship = new Friendship(2L, 1L, date);
        var snapshot = userRepo.findAll();

        assertDoesNotThrow(new Executable()
        {
            @Override
            public void execute()
            {
                validator.validate(friendship);
            }
        });
        assertEquals(snapshot, userRepo.findAll());
        assertSame(first, userRepo.findOne(1L).orElseThrow());
        assertSame(second, userRepo.findOne(2L).orElseThrow());
        assertEquals(date, friendship.getCreatedAt());
    }

    @Test
    void testNullFriendshipValidation()
    {
        var validator = new FriendshipValidator(new InMemoryUserRepository());
        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                validator.validate(null);
            }
        });
    }

    @Test
    void testNonexistentUsersValidation()
    {
        // Test neither user, only the first user, and only the second user existing.
        for(int existingUser = 0; existingUser <= 2; existingUser++)
        {
            var userRepo = new InMemoryUserRepository();
            if(existingUser != 0)
            {
                userRepo.save(new User((long) existingUser, "Ana", "Popescu"));
            }
            var validator = new FriendshipValidator(userRepo);
            var snapshot = userRepo.findAll();

            assertThrows(IllegalArgumentException.class, new Executable()
            {
                @Override
                public void execute()
                {
                    validator.validate(new Friendship(1L, 2L));
                }
            });
            assertEquals(snapshot, userRepo.findAll());
        }
    }

    @Test
    void testValidationUsesCurrentUsers()
    {
        var userRepo = new InMemoryUserRepository();
        userRepo.save(new User(1L, "Ana", "Popescu"));
        userRepo.save(new User(2L, "Sebi", "Ionescu"));
        var validator = new FriendshipValidator(userRepo);
        Friendship friendship = new Friendship(1L, 2L);
        validator.validate(friendship);
        userRepo.delete(2L);

        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                validator.validate(friendship);
            }
        });
        assertTrue(userRepo.findOne(1L).isPresent());
        assertTrue(userRepo.findOne(2L).isEmpty());
    }

    @Test
    void testNullUserRepositoryValidation()
    {
        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                new FriendshipValidator(null);
            }
        });
    }
}
