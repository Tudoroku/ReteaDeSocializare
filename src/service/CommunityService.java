package service;

import repository.FriendshipRepository;
import repository.UserRepository;

import java.util.*;

public class CommunityService {
    private final UserRepository userRepo;
    private final FriendshipRepository friendshipRepo;

    public CommunityService(UserRepository userRepo, FriendshipRepository friendshipRepo) {
        this.userRepo = userRepo;
        this.friendshipRepo = friendshipRepo;
    }

    public Map<Long, Set<Long>> buildGraph() {
        Map<Long, Set<Long>> graph = new HashMap<>();

        for (var user : userRepo.findAll()) {
            graph.put(user.getId(), new HashSet<>());
        }

        for (var friendship : friendshipRepo.findAll()) {
            Long id1 = friendship.getId().firstId();
            Long id2 = friendship.getId().secondId();

            graph.get(id1).add(id2);
            graph.get(id2).add(id1);
        }

        return graph;
    }

    private void dfs(Long start, Map<Long, Set<Long>> graph, Set<Long> visited)
    {
        Deque<Long> stack = new ArrayDeque<>();

        stack.push(start);
        visited.add(start);

        while (!stack.isEmpty()) {
            Long current = stack.pop();

            for (Long neighbor : graph.get(current)) {
                if (visited.add(neighbor)) {
                    stack.push(neighbor);
                }
            }
        }
    }

    public int getNumberOfCommunities()
    {
        var network = buildGraph();
        if(network.isEmpty())
        {
            return 0;
        }
        Set<Long> visited = new HashSet<>();
        int communities = 0;
        Set<Long> vertices = network.keySet();

        for(var vertex : vertices)
        {
            if(!visited.contains(vertex))
            {
                dfs(vertex, network, visited);
                communities++;
            }
        }

        return communities;
    }
}
