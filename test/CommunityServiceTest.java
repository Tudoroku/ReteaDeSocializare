import domain.Friendship;
import domain.User;
import org.junit.jupiter.api.Test;
import repository.InMemoryFriendshipRepository;
import repository.InMemoryUserRepository;
import service.CommunityService;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CommunityServiceTest {

    @Test
    void testBuildGraph()
    {
        var userRepo = new InMemoryUserRepository();
        var friendshipRepo = new InMemoryFriendshipRepository();
        var service = new CommunityService(userRepo, friendshipRepo);

        User user1 = new User(1L, "Ana", LocalDate.of(2000, 1, 1));
        User user2 = new User(2L, "Sebi", LocalDate.of(2001, 2, 2));
        User user3 = new User(3L, "Chris", LocalDate.of(2002, 3, 3));
        User user4 = new User(4L, "Stacy", LocalDate.of(2003, 4, 4));

        userRepo.save(user1);
        userRepo.save(user2);
        userRepo.save(user3);
        userRepo.save(user4);

        // Salvare prietenii
        friendshipRepo.save(new Friendship(1L, 2L));
        friendshipRepo.save(new Friendship(2L, 3L));

        var graph = service.buildGraph();

        // Test number of vertices
        assertEquals(4, graph.size());

        // Test adjacency lists
        assertEquals(Set.of(2L), graph.get(1L));
        assertEquals(Set.of(1L, 3L), graph.get(2L));
        assertEquals(Set.of(2L), graph.get(3L));

        // Test isolated user
        assertTrue(graph.get(4L).isEmpty());
    }

    @Test
    void testGetNumberOfCommunities()
    {
        var userRepo = new InMemoryUserRepository();
        var friendshipRepo = new InMemoryFriendshipRepository();
        var service = new CommunityService(userRepo, friendshipRepo);

        // Test empty network
        assertEquals(0, service.getNumberOfCommunities());

        User user1 = new User(1L, "Ana", LocalDate.of(2000, 1, 1));
        User user2 = new User(2L, "Sebi", LocalDate.of(2001, 2, 2));
        User user3 = new User(3L, "Chris", LocalDate.of(2002, 3, 3));
        User user4 = new User(4L, "Stacy", LocalDate.of(2003, 4, 4));
        User user5 = new User(5L, "Bernard", LocalDate.of(2004, 5, 5));
        User user6 = new User(6L, "George", LocalDate.of(2005, 6, 6));

        userRepo.save(user1);
        userRepo.save(user2);
        userRepo.save(user3);
        userRepo.save(user4);
        userRepo.save(user5);
        userRepo.save(user6);

        // Test all users isolated
        assertEquals(6, service.getNumberOfCommunities());

        // Community 1: (1, 2, 3)
        friendshipRepo.save(new Friendship(1L, 2L));
        friendshipRepo.save(new Friendship(2L, 3L));

        // Community 2: (4, 5)
        friendshipRepo.save(new Friendship(4L, 5L));

        // Community 3: (6) - isolated user
        assertEquals(3, service.getNumberOfCommunities());

        // Connect communities 1 and 2
        friendshipRepo.save(new Friendship(3L, 4L));

        // Now we have 2 communities
        assertEquals(2, service.getNumberOfCommunities());

        // Connect isolated user to the network
        friendshipRepo.save(new Friendship(5L, 6L));

        // All users belong to one community
        assertEquals(1, service.getNumberOfCommunities());
    }
}
