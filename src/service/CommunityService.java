package service;

import repository.FriendshipRepository;
import repository.UserRepository;
import domain.Friendship;
import domain.User;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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
        Map<Long, Set<Long>> graph = StreamSupport
                .stream(userRepo.findAll().spliterator(), false)
                .map(User::getId)
                .distinct()
                .collect(Collectors.toMap(userId -> userId, userId -> new HashSet<>()));

        StreamSupport.stream(friendshipRepo.findAll().spliterator(), false)
                .map(Friendship::getId)
                .forEach(friendshipId -> {
                    Long firstId = friendshipId.firstId();
                    Long secondId = friendshipId.secondId();

                    graph.get(firstId).add(secondId);
                    graph.get(secondId).add(firstId);
                });

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

            graph.get(current).stream()
                    .filter(neighbor -> !visited.contains(neighbor))
                    .forEach(neighbor -> {
                        visited.add(neighbor);
                        stack.push(neighbor);
                    });
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
    public List<Set<Long>> getCommunities() {
        return getCommunities(buildGraph());
    }

    /**
     * Finds the connected components of an existing graph.
     *
     * @param graph the adjacency map of the social network
     * @return a mutable list containing each connected component once
     */
    private List<Set<Long>> getCommunities(Map<Long, Set<Long>> graph) {
        class Components {
            final List<Set<Long>> communities = new ArrayList<>();
            final Set<Long> visited = new HashSet<>();
        }

        Components result = graph.keySet().stream().collect(
                Components::new,
                (components, vertex) -> {
                    if (!components.visited.contains(vertex)) {
                        components.communities.add(dfs(vertex, graph, components.visited));
                    }
                },
                (left, right) -> right.communities.forEach(community -> {
                    if (left.visited.addAll(community)) {
                        left.communities.add(community);
                    }
                })
        );

        return result.communities;
    }

    /**
     * Recomputes the connected components and counts them.
     * @return the number of components, including isolated users; zero for an empty network
     * @throws NullPointerException if dependencies or friendship endpoints are missing
     * @throws UnsupportedOperationException if repository retrieval is not implemented
     */
    public int getNumberOfCommunities() {
        return Math.toIntExact(getCommunities().stream().count());
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
     * Returns the community containing the longest simple path.
     * If several communities tie, the first encountered community is returned.
     *
     * @return the selected community, or an empty set if there are no communities
     * @throws UnsupportedOperationException if a repository cannot list its entities
     */
    public Set<Long> getMostSociableCommunity() {
        var graph = buildGraph();

        return getCommunities(graph).stream()
                .map(community -> Map.entry(community, longestPathInCommunity(community, graph)))
                .reduce((best, current) -> best.getValue() >= current.getValue() ? best : current)
                .map(Map.Entry::getKey)
                .orElseGet(Set::of);
    }

    /**
     * Finds the length of the longest simple path in a community.
     *
     * @param community the user IDs in the community
     * @param graph the adjacency map of the social network
     * @return the maximum path length in edges, or zero if the community is empty
     */
    private int longestPathInCommunity(Set<Long> community, Map<Long, Set<Long>> graph) {
        return community.stream()
                .mapToInt(userId -> longestPathDFS(userId, graph, new HashSet<>()))
                .max()
                .orElse(0);
    }
}
