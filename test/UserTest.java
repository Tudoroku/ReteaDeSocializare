import domain.User;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    @Test
    void testConstructorAndGetters()
    {
        User user = new User(1L, "Ana", "Popescu");
        assertEquals(1L, user.getId());
        assertEquals("Ana", user.getFirstName());
        assertEquals("Popescu", user.getLastName());
    }

    @Test
    void testEqualsAndHashCodeDependOnlyOnId()
    {
        User first = new User(1L, "Ana", "Popescu");
        User second = new User(1L, "Sebi", "Ionescu");
        User third = new User(1L, "Chris", "Georgescu");

        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(second, third);
        assertEquals(first, third);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new User(2L, "Ana", "Popescu"));
        assertNotEquals(first, null);
        assertNotEquals(first, "Ana Popescu");

        Set<User> users = new HashSet<>();
        users.add(first);
        users.add(second);
        assertEquals(1, users.size());
    }

    @Test
    void testNullIdEquality()
    {
        // Validation belongs to UserValidator; the entity permits a null ID.
        User first = new User(null, "Ana", "Popescu");
        User second = new User(null, "Sebi", "Ionescu");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new User(1L, "Ana", "Popescu"));
    }

    @Test
    void testToString()
    {
        User user = new User(1L, "Ana", "Popescu");
        assertEquals("User{id=1, first name='Ana', last name='Popescu'}", user.toString());
    }
}
