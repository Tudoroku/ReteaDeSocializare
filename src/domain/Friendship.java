package domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents an undirected friendship with a normalized ID pair and a non-null
 * local creation date-time. Construction checks IDs and the date, but not user
 * existence. Equality and hashing ignore the creation date.
 */
public class Friendship implements HasId<FriendshipId>{
    private final FriendshipId id;
    private final LocalDateTime createdAt;

    /**
     * Creates a friendship using the current system local date-time.
     * @param firstId one non-null, positive user identifier
     * @param secondId the other non-null, positive identifier, distinct from firstId
     * @throws IllegalArgumentException if an ID is null or nonpositive, or IDs are equal
     */
    public Friendship(Long firstId, Long secondId) {
        this(firstId, secondId, LocalDateTime.now());
    }

    /**
     * Normalizes and checks the IDs, then stores the supplied creation date-time.
     * @param firstId one non-null, positive user identifier
     * @param secondId the other non-null, positive identifier, distinct from firstId
     * @param createdAt the non-null local creation date-time; past and future values are accepted
     * @throws IllegalArgumentException if an ID is null or nonpositive, IDs are equal,
     *         or createdAt is null
     */
    public Friendship(Long firstId, Long secondId, LocalDateTime createdAt)
    {
        this.id = new FriendshipId(firstId, secondId);
        if(createdAt == null)
        {
            throw new IllegalArgumentException("Friendship date cannot be null");
        }
        this.createdAt = createdAt;
    }

    /**
     * {@inheritDoc}
     * @return the non-null normalized pair created by the constructor
     */
    @Override
    public FriendshipId getId()
    {
        return this.id;
    }

    /**
     * Returns the stored creation date-time, without a time-zone conversion.
     * @return the non-null local creation date-time
     */
    public LocalDateTime getCreatedAt()
    {
        return this.createdAt;
    }

    /**
     * Compares normalized pairs only when objects have the same runtime class.
     * The creation date-time is ignored.
     * @param other the object to compare, possibly null
     * @return true for the same object or the same runtime class with an equal pair
     */
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

    /**
     * Returns a diagnostic description containing both IDs and the creation date-time.
     * @return the friendship description
     */
    @Override
    public String toString()
    {
        return "Friendship{" +
                "firstId=" + id.firstId() +
                ", secondId=" + id.secondId() +
                ", createdAt=" + createdAt +
                '}';
    }
    /**
     * Computes a hash from the normalized ID pair alone.
     * @return the pair's hash, consistently with equality
     */
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
