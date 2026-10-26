package domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class Friendship implements HasId<FriendshipId>{
    private final FriendshipId id;
    private final LocalDateTime createdAt;

    public Friendship(Long firstId, Long secondId) {
        this(firstId, secondId, LocalDateTime.now());
    }

    public Friendship(Long firstId, Long secondId, LocalDateTime createdAt)
    {
        this.id = new FriendshipId(firstId, secondId);
        if(createdAt == null)
        {
            throw new IllegalArgumentException("Friendship date cannot be null");
        }
        this.createdAt = createdAt;
    }

    @Override
    public FriendshipId getId()
    {
        return this.id;
    }

    public LocalDateTime getCreatedAt()
    {
        return this.createdAt;
    }

    @Override
    public boolean equals(Object other)
    {
        if(this == other)
        {
            return true;
        }
        if(other == null || this.getClass() != other.getClass())
        {
            return false;
        }
        return this.id.equals(((Friendship) other).id);
    }

    @Override
    public String toString()
    {
        return "Friendship{" +
                "firstId=" + id.firstId() +
                ", secondId=" + id.secondId() +
                ", createdAt=" + createdAt +
                '}';
    }
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
