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

    private Set<Long> dfs(Long start, Map<Long, Set<Long>> graph, Set<Long> visited)
    {
        Set<Long> component = new HashSet<>();
        Deque<Long> stack = new ArrayDeque<>();

        stack.push(start);
        visited.add(start);

        while (!stack.isEmpty()) {
            Long current = stack.pop();
            component.add(current);

            for (Long neighbor : graph.get(current)) {
                if (visited.add(neighbor)) {
                    stack.push(neighbor);
                }
            }
        }

        return component;
    }

    public List<Set<Long>> getCommunities()
    {
        var graph = buildGraph();

        List<Set<Long>> communities = new ArrayList<>();
        Set<Long> visited = new HashSet<>();

        for (Long vertex : graph.keySet()) {
            if (!visited.contains(vertex)) {
                communities.add(dfs(vertex, graph, visited));
            }
        }

        return communities;
    }

    public int getNumberOfCommunities()
    {
        return getCommunities().size();
    }

    private int longestPathDFS(Long current, Map<Long, Set<Long>> graph, Set<Long> visited)
    {
        visited.add(current);

        int maxLength = 0;

        for (Long neighbor : graph.get(current))
        {
            if (!visited.contains(neighbor))
            {
                int length = 1 + longestPathDFS(neighbor, graph, visited);

                maxLength = Math.max(maxLength, length);
            }
        }

        visited.remove(current);

        return maxLength;
    }

    public Set<Long> getMostSociableCommunity()
    {
        var graph = buildGraph();
        List<Set<Long>> communities = getCommunities();
        int maxGlobal = -1;

        Set<Long> maxCom = Set.of();
        Set<Long> visited = new HashSet<>();

        for(var community : communities)
        {
            int maxLocal = Integer.MIN_VALUE;
            for(var node : community)
            {
                int distance = longestPathDFS(node, graph, visited);
                if(distance > maxLocal)
                {
                    maxLocal = distance;
                }
            }
            if(maxLocal > maxGlobal)
            {
                maxGlobal = maxLocal;
                maxCom = community;
            }
        }

        return maxCom;
    }
}
