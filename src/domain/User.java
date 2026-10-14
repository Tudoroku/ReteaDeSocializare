package domain;

import java.time.LocalDate;
import java.util.Objects;

public class User implements HasId<Long>{
    private final Long id;
    private final String name;
    private final LocalDate birthday;

    public User(Long id, String name, LocalDate birthday)
    {
        this.id = id;
        this.name = name;
        this.birthday = birthday;
    }

    @Override
    public Long getId()
    {
        return this.id;
    }

    public String getName()
    {
        return this.name;
    }

    public LocalDate getBirthday()
    {
        return this.birthday;
    }

    @Override
    public String toString()
    {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", birthday=" + birthday +
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
        return this.id.hashCode();
    }
}
