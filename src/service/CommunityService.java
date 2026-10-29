package service;

import repository.FriendshipRepository;
import repository.UserRepository;

import java.util.*;

/**
 * Derives undirected communities from the current user and friendship repositories.
 * Isolated users form single-member communities. Analysis does not mutate stored
 * entities and assumes every friendship endpoint is present among the users.
 */
public class CommunityService {
    private final UserRepository userRepo;
    private final FriendshipRepository friendshipRepo;

    /**
     * Stores repositories without validating dependencies or graph consistency.
     * @param userRepo the repository supplying graph vertices
     * @param friendshipRepo the repository supplying graph edges
     */
    public CommunityService(UserRepository userRepo, FriendshipRepository friendshipRepo) {
        this.userRepo = userRepo;
        this.friendshipRepo = friendshipRepo;
    }

    /**
     * Builds fresh mutable adjacency sets from the current repository contents.
     * Adds both directions for each friendship; repeated edges collapse in sets.
     * @return a mutable user-ID to neighbor-ID map, empty when both repositories are empty
     * @throws NullPointerException if a dependency is null or a friendship refers
     *         to a user missing from the graph
     * @throws UnsupportedOperationException if repository retrieval is not implemented
     */
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

    /**
     * Computes connected components of a newly built graph.
     * @return a fresh mutable list of mutable ID sets, empty for an empty network;
     *         component and member order are unspecified
     * @throws NullPointerException if dependencies or friendship endpoints are missing
     * @throws UnsupportedOperationException if repository retrieval is not implemented
     */
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

    /**
     * Recomputes the connected components and counts them.
     * @return the number of components, including isolated users; zero for an empty network
     * @throws NullPointerException if dependencies or friendship endpoints are missing
     * @throws UnsupportedOperationException if repository retrieval is not implemented
     */
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

    /**
     * Selects the component containing the longest simple path, measured in edges.
     * Ties retain the first encountered component, whose iteration order is
     * unspecified. Builds the graph and components in separate repository reads,
     * so they must describe the same network during this call.
     * @return the selected mutable component set, or an unmodifiable empty set
     *         when there are no users
     * @throws NullPointerException if dependencies or friendship endpoints are missing,
     *         or the separate reads produce incompatible graphs
     * @throws UnsupportedOperationException if repository retrieval is not implemented
     */
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
