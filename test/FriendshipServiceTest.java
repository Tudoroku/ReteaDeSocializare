import domain.FriendshipId;
import domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import repository.InMemoryFriendshipRepository;
import repository.InMemoryUserRepository;
import service.FriendshipService;


import static org.junit.jupiter.api.Assertions.*;

public class FriendshipServiceTest {

    @Test
    void testAddFriendship()
    {
        var userRepo = new InMemoryUserRepository();
        var friendshipRepo = new InMemoryFriendshipRepository();
        var service = new FriendshipService(userRepo, friendshipRepo);

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

        userRepo.save(user1);
        userRepo.save(user2);

        // Test valid friendship
        assertDoesNotThrow(new Executable()
        {
            @Override
            public void execute()
            {
                service.addFriendship(1L, 2L);
            }
        });

        assertTrue(
                friendshipRepo.findOne(new FriendshipId(1L, 2L)).isPresent()
        );

        // Test duplicate friendship with reversed IDs
        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                service.addFriendship(2L, 1L);
            }
        });

        // Test friendship with nonexistent user
        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                service.addFriendship(1L, 99L);
            }
        });

        assertTrue(
                friendshipRepo.findOne(new FriendshipId(1L, 99L)).isEmpty()
        );

        // Test friendship with same user
        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                service.addFriendship(1L, 1L);
            }
        });

        // Test null ID
        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                service.addFriendship(null, 2L);
            }
        });

        // Test that original friendship still exists
        assertTrue(
                friendshipRepo.findOne(new FriendshipId(1L, 2L)).isPresent()
        );
    }

    @Test
    void testDeleteFriendship()
    {
        var userRepo = new InMemoryUserRepository();
        var friendshipRepo = new InMemoryFriendshipRepository();
        var service = new FriendshipService(userRepo, friendshipRepo);

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

        userRepo.save(user1);
        userRepo.save(user2);

        service.addFriendship(1L, 2L);

        // Test delete existing friendship with reversed IDs
        assertDoesNotThrow(new Executable()
        {
            @Override
            public void execute()
            {
                service.deleteFriendship(2L, 1L);
            }
        });

        assertTrue(
                friendshipRepo.findOne(new FriendshipId(1L, 2L)).isEmpty()
        );

        // Test delete nonexistent friendship
        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                service.deleteFriendship(1L, 2L);
            }
        });

        // Test delete with invalid IDs
        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                service.deleteFriendship(1L, 1L);
            }
        });

        // Verify users were not deleted
        assertTrue(userRepo.findOne(1L).isPresent());
        assertTrue(userRepo.findOne(2L).isPresent());
    }
}
