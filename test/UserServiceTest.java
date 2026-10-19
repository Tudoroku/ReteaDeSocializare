import static org.junit.jupiter.api.Assertions.*;

import domain.Friendship;
import domain.FriendshipId;
import domain.User;
import org.junit.jupiter.api.Test;
import repository.FriendshipRepository;
import repository.InMemoryFriendshipRepository;
import repository.InMemoryUserRepository;
import service.UserService;
import validation.UserValidator;

import java.time.LocalDate;

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
                LocalDate.of(2000,1,1)
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
                LocalDate.of(2000, 2 ,2 )
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
                LocalDate.of(2002, 2, 3)
        );

        assertThrows(IllegalArgumentException.class, () ->
        {
           service.addUser(bernard);
        });
        assertSame(chris, repo.findOne(1L).orElseThrow());
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
                LocalDate.of(2000, 1, 1)
        );

        User user2 = new User(
                2L,
                "Stacy",
                LocalDate.of(2001, 2, 2)
        );

        User user3 = new User(
                3L,
                "Bernard",
                LocalDate.of(2002, 3, 3)
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
