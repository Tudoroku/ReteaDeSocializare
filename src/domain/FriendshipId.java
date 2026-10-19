package domain;

public record FriendshipId(Long firstId, Long secondId) {
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
