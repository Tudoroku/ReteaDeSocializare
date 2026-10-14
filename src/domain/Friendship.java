package domain;

public class Friendship implements HasId<FriendshipId>{
    private final FriendshipId id;

    public Friendship(Long firstId, Long secondId)
    {
        this.id = new FriendshipId(firstId, secondId);
    }

    public FriendshipId getId()
    {
        return this.id;
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
                '}';
    }
    @Override
    public int hashCode()
    {
        return (this.id.firstId().hashCode() + this.id.secondId().hashCode());
    }
}
