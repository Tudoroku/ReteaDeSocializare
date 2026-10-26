package domain;

import java.util.Objects;

public class User implements HasId<Long>{
    private final Long id;
    private final String firstName;
    private final String lastName;

    public User(Long id, String firstName, String lastName)
    {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public Long getId()
    {
        return this.id;
    }

    public String getFirstName()
    {
        return this.firstName;
    }

    public String getLastName()
    {
        return this.lastName;
    }

    @Override
    public String toString()
    {
        return "User{" +
                "id=" + id +
                ", first name='" + firstName + '\'' +
                ", last name='" + lastName + '\'' +
                '}';
    }

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

    @Override
    public int hashCode()
    {
        return Objects.hashCode(this.id);
    }
}
