package domain;

import java.util.Objects;

/**
 * Holds a user's identifier and names exactly as supplied, without validation or
 * trimming. Equality and hashing use only the identifier, including a null ID;
 * name checks belong to {@link validation.UserValidator}.
 */
public class User implements HasId<Long>{
    private final Long id;
    private final String firstName;
    private final String lastName;

    /**
     * Stores the supplied values without validating them.
     * @param id the identifier, which may be null or nonpositive at construction
     * @param firstName the first name, which may be null or blank at construction
     * @param lastName the last name, which may be null or blank at construction
     */
    public User(Long id, String firstName, String lastName)
    {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    /**
     * {@inheritDoc}
     * @return the original identifier, possibly null
     */
    @Override
    public Long getId()
    {
        return this.id;
    }

    /**
     * Returns the stored first name without trimming or normalization.
     * @return the original first name, possibly null
     */
    public String getFirstName()
    {
        return this.firstName;
    }

    /**
     * Returns the stored last name without trimming or normalization.
     * @return the original last name, possibly null
     */
    public String getLastName()
    {
        return this.lastName;
    }

    /**
     * Returns a diagnostic representation including the ID and both names.
     * @return the user description, with null fields represented as {@code null}
     */
    @Override
    public String toString()
    {
        return "User{" +
                "id=" + id +
                ", first name='" + firstName + '\'' +
                ", last name='" + lastName + '\'' +
                '}';
    }

    /**
     * Compares identifiers with another {@code User}, including subclasses.
     * Two users with null identifiers compare equal; names are ignored.
     * @param other the object to compare, possibly null
     * @return true for the same object or a user with an equal identifier
     */
    @Override
    public boolean equals(Object other)
    {
        if(this == other)
        {
            return true;
        }
        if(!(other instanceof User user))
        {
            return false;
        }
        return Objects.equals(this.id, user.id);
    }

    /**
     * Computes a hash from the identifier alone, consistently with equality.
     * @return the identifier hash, or zero when the identifier is null
     */
    @Override
    public int hashCode()
    {
        return Objects.hashCode(this.id);
    }
}
