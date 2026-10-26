import static org.junit.jupiter.api.Assertions.*;

import domain.Friendship;
import domain.FriendshipId;
import domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import repository.FriendshipRepository;
import repository.InMemoryFriendshipRepository;
import repository.InMemoryUserRepository;
import service.UserService;
import validation.UserValidator;


public class UserServiceTest {

    @Test
    void testAddUser()
    {
        var repo = new InMemoryUserRepository();
        var validator = new UserValidator();
        var friendshipRepo = new InMemoryFriendshipRepository();
        var service = new UserService(repo, validator, friendshipRepo);

        //Test valid User
        User chris = new User(
                1L,
                "Chris",
                "Popescu"
        );

        assertDoesNotThrow(() ->
        {
            service.addUser(chris);
        });
        assertSame(chris, repo.findOne(1L).orElseThrow());

        //Test Invalid User
        User stacy = new User(
                2L,
                "     ",
                "Popescu"
        );

        var snapshot = repo.findAll();

        assertThrows(IllegalArgumentException.class, () ->
        {
           service.addUser(stacy);
        });

        assertEquals(snapshot, repo.findAll());

        User bernard = new User(
                1L,
                "Bernard",
                "Popescu"
        );

        assertThrows(IllegalArgumentException.class, () ->
        {
           service.addUser(bernard);
        });
        assertSame(chris, repo.findOne(1L).orElseThrow());
    }

    @Test
    void testInvalidNamesDoNotChangeRepository()
    {
        var repo = new InMemoryUserRepository();
        var validator = new UserValidator();
        var friendshipRepo = new InMemoryFriendshipRepository();
        var service = new UserService(repo, validator, friendshipRepo);

        User existingUser = new User(1L, "Chris", "Popescu");
        service.addUser(existingUser);
        var snapshot = repo.findAll();

        User[] invalidUsers = {
                new User(2L, null, "Popescu"),
                new User(2L, "", "Popescu"),
                new User(2L, " \t\n ", "Popescu"),
                new User(2L, "Stacy", null),
                new User(2L, "Stacy", ""),
                new User(2L, "Stacy", " \t\n ")
        };

        for(User user : invalidUsers)
        {
            assertThrows(IllegalArgumentException.class, new Executable()
            {
                @Override
                public void execute()
                {
                    service.addUser(user);
                }
            });
            assertEquals(snapshot, repo.findAll());
            assertSame(existingUser, repo.findOne(1L).orElseThrow());
            assertTrue(repo.findOne(2L).isEmpty());
        }
    }

    @Test
    void testDeleteUser()
    {
        var repo = new InMemoryUserRepository();
        var validator = new UserValidator();
        var friendshipRepo = new InMemoryFriendshipRepository();
        var service = new UserService(repo, validator, friendshipRepo);

        User user1 = new User(
                1L,
                "Chris",
                "Popescu"
        );

        User user2 = new User(
                2L,
                "Stacy",
                "Popescu"
        );

        User user3 = new User(
                3L,
                "Bernard",
                "Popescu"
        );

        // Salvare utilizatori
        service.addUser(user1);
        service.addUser(user2);
        service.addUser(user3);

        // Salvare prietenii
        friendshipRepo.save(new Friendship(1L, 2L));
        friendshipRepo.save(new Friendship(1L, 3L));
        friendshipRepo.save(new Friendship(2L, 3L));

        // Test delete existing user
        assertDoesNotThrow(() ->
        {
            service.deleteUser(1L);
        });

        // Verificare utilizator sters
        assertTrue(repo.findOne(1L).isEmpty());

        // Verificare prietenii sterse
        assertTrue(
                friendshipRepo.findOne(new FriendshipId(1L, 2L)).isEmpty()
        );

        assertTrue(
                friendshipRepo.findOne(new FriendshipId(1L, 3L)).isEmpty()
        );

        // Verificare prietenie ramasa
        assertTrue(
                friendshipRepo.findOne(new FriendshipId(2L, 3L)).isPresent()
        );

        // Verificare utilizatori ramasi
        assertSame(user2, repo.findOne(2L).orElseThrow());
        assertSame(user3, repo.findOne(3L).orElseThrow());

        // Test delete nonexistent user
        assertThrows(IllegalArgumentException.class, () ->
        {
            service.deleteUser(99L);
        });
    }

}
