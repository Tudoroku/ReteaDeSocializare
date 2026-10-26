import domain.Friendship;
import domain.User;
import org.junit.jupiter.api.Test;
import repository.InMemoryFriendshipRepository;
import repository.InMemoryUserRepository;
import service.CommunityService;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CommunityServiceTest {

    @Test
    void testBuildGraph()
    {
        var userRepo = new InMemoryUserRepository();
        var friendshipRepo = new InMemoryFriendshipRepository();
        var service = new CommunityService(userRepo, friendshipRepo);

        User user1 = new User(1L, "Ana", "Popescu");
        User user2 = new User(2L, "Sebi", "Popescu");
        User user3 = new User(3L, "Chris", "Popescu");
        User user4 = new User(4L, "Stacy", "Popescu");

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

        User user1 = new User(1L, "Ana", "Popescu");
        User user2 = new User(2L, "Sebi", "Popescu");
        User user3 = new User(3L, "Chris", "Popescu");
        User user4 = new User(4L, "Stacy", "Popescu");
        User user5 = new User(5L, "Bernard", "Popescu");
        User user6 = new User(6L, "George", "Popescu");

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

    @Test
    void testGetCommunities()
    {
        var userRepo = new InMemoryUserRepository();
        var friendshipRepo = new InMemoryFriendshipRepository();
        var service = new CommunityService(userRepo, friendshipRepo);

        // Test empty network
        assertTrue(service.getCommunities().isEmpty());

        User user1 = new User(1L, "Ana", "Popescu");
        User user2 = new User(2L, "Sebi", "Popescu");
        User user3 = new User(3L, "Chris", "Popescu");
        User user4 = new User(4L, "Stacy", "Popescu");
        User user5 = new User(5L, "Bernard", "Popescu");
        User user6 = new User(6L, "George", "Popescu");

        // Salvare utilizatori
        userRepo.save(user1);
        userRepo.save(user2);
        userRepo.save(user3);
        userRepo.save(user4);
        userRepo.save(user5);
        userRepo.save(user6);

        // Community 1: (1, 2, 3)
        friendshipRepo.save(new Friendship(1L, 2L));
        friendshipRepo.save(new Friendship(2L, 3L));

        // Community 2: (4, 5)
        friendshipRepo.save(new Friendship(4L, 5L));

        // Community 3: (6) - isolated user

        var communities = service.getCommunities();

        // Test number of communities
        assertEquals(3, communities.size());

        // Test community members, regardless of order
        Set<Set<Long>> expectedCommunities = Set.of(
                Set.of(1L, 2L, 3L),
                Set.of(4L, 5L),
                Set.of(6L)
        );

        Set<Set<Long>> actualCommunities = new HashSet<>(communities);

        assertEquals(expectedCommunities, actualCommunities);

        // Connect two communities
        friendshipRepo.save(new Friendship(3L, 4L));

        assertEquals(
                Set.of(Set.of(1L, 2L, 3L, 4L, 5L), Set.of(6L)),
                new HashSet<>(service.getCommunities())
        );

        // Connect isolated user
        friendshipRepo.save(new Friendship(5L, 6L));

        assertEquals(
                Set.of(Set.of(1L, 2L, 3L, 4L, 5L, 6L)),
                new HashSet<>(service.getCommunities())
        );
    }

    @Test
    void testMostSociableCommunity()
    {
        var userRepo = new InMemoryUserRepository();
        var friendshipRepo = new InMemoryFriendshipRepository();
        var service = new CommunityService(userRepo, friendshipRepo);

        // Salvare utilizatori
        for(long id = 1; id <= 10; id++)
        {
            userRepo.save(new User(
                    id,
                    "User" + id,
                    "Popescu"
            ));
        }

        // Community 1: Star (1, 2, 3, 4, 5, 6)
        // Longest path = 2 edges
        friendshipRepo.save(new Friendship(1L, 2L));
        friendshipRepo.save(new Friendship(1L, 3L));
        friendshipRepo.save(new Friendship(1L, 4L));
        friendshipRepo.save(new Friendship(1L, 5L));
        friendshipRepo.save(new Friendship(1L, 6L));

        // Community 2: Chain (7, 8, 9, 10)
        // Longest path = 3 edges
        friendshipRepo.save(new Friendship(7L, 8L));
        friendshipRepo.save(new Friendship(8L, 9L));
        friendshipRepo.save(new Friendship(9L, 10L));

        // Test community with longest path
        Set<Long> expected = Set.of(7L, 8L, 9L, 10L);

        assertEquals(expected, service.getMostSociableCommunity());
    }

    @Test
    void testMostSociableCommunityEmptyGraph()
    {
        var userRepo = new InMemoryUserRepository();
        var friendshipRepo = new InMemoryFriendshipRepository();
        var service = new CommunityService(userRepo, friendshipRepo);

        // Test empty graph
        assertEquals(Set.of(), service.getMostSociableCommunity());

        // Test single isolated user
        User user = new User(
                1L,
                "Chris",
                "Popescu"
        );

        userRepo.save(user);

        assertEquals(
                Set.of(1L),
                service.getMostSociableCommunity()
        );
    }

    @Test
    void testMostSociableCommunityWithCycle()
    {
        var userRepo = new InMemoryUserRepository();
        var friendshipRepo = new InMemoryFriendshipRepository();
        var service = new CommunityService(userRepo, friendshipRepo);

        // Salvare utilizatori
        for(long id = 1; id <= 5; id++)
        {
            userRepo.save(new User(
                    id,
                    "User" + id,
                    "Popescu"
            ));
        }

        // Community 1: Cycle (1, 2, 3)
        friendshipRepo.save(new Friendship(1L, 2L));
        friendshipRepo.save(new Friendship(2L, 3L));
        friendshipRepo.save(new Friendship(3L, 1L));

        // Community 2: (4, 5)
        friendshipRepo.save(new Friendship(4L, 5L));

        // Cycle has longest simple path of 2 edges
        // Second community has longest path of 1 edge
        Set<Long> expected = Set.of(1L, 2L, 3L);

        assertEquals(expected, service.getMostSociableCommunity());
    }


}
