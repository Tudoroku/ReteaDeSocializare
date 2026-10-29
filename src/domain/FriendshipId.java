package domain;

/**
 * Identifies an unordered pair of distinct positive user IDs. Construction
 * stores the smaller ID first, so reversed inputs produce equal records.
 * @param firstId the smaller normalized identifier returned by {@code firstId()}
 * @param secondId the larger normalized identifier returned by {@code secondId()}
 */
public record FriendshipId(Long firstId, Long secondId) {
    /**
     * Checks the identifiers and stores them in ascending order.
     * @param firstId one non-null, positive identifier
     * @param secondId the other non-null, positive identifier, distinct from firstId
     * @throws IllegalArgumentException if either identifier is null or nonpositive,
     *         or the identifiers are equal
     */
    public FriendshipId(Long firstId, Long secondId) {
        if(firstId == null || secondId == null)
        {
            throw new IllegalArgumentException("Ids cannot be null");
        }
        if(firstId <= 0 || secondId <= 0)
        {
            throw new IllegalArgumentException("Ids must be positive");
        }
        if(firstId.equals(secondId))
        {
            throw new IllegalArgumentException("Ids cannot be equal");
        }
        if(secondId < firstId)
        {
            Long temp = firstId;
            firstId = secondId;
            secondId = temp;
        }
        this.firstId = firstId;
        this.secondId = secondId;
    }
}
