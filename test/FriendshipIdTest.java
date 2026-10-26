import domain.FriendshipId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class FriendshipIdTest {

    @Test
    void testIdNormalization()
    {
        FriendshipId ordered = new FriendshipId(1L, 2L);
        FriendshipId reversed = new FriendshipId(2L, 1L);

        assertEquals(1L, ordered.firstId());
        assertEquals(2L, ordered.secondId());
        assertEquals(ordered.firstId(), reversed.firstId());
        assertEquals(ordered.secondId(), reversed.secondId());

        FriendshipId large = new FriendshipId(Long.MAX_VALUE, 1L);
        assertEquals(1L, large.firstId());
        assertEquals(Long.MAX_VALUE, large.secondId());
    }

    @Test
    void testEqualsAndHashCode()
    {
        FriendshipId first = new FriendshipId(1L, 2L);
        FriendshipId second = new FriendshipId(2L, 1L);
        FriendshipId third = new FriendshipId(1L, 2L);

        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(second, third);
        assertEquals(first, third);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new FriendshipId(1L, 3L));
        assertNotEquals(first, null);
        assertNotEquals(first, "1;2");

        Set<FriendshipId> ids = new HashSet<>();
        ids.add(first);
        ids.add(second);
        assertEquals(1, ids.size());
    }

    @Test
    void testInvalidIds()
    {
        Long[][] invalidIds = {
                {null, 2L}, {1L, null}, {null, null},
                {0L, 2L}, {1L, 0L}, {0L, 0L},
                {-1L, 2L}, {1L, -2L}, {-1L, -2L},
                {Long.MIN_VALUE, 2L}, {1L, 1L},
                {Long.MAX_VALUE, Long.MAX_VALUE}
        };

        for(Long[] ids : invalidIds)
        {
            assertThrows(IllegalArgumentException.class, new Executable()
            {
                @Override
                public void execute()
                {
                    new FriendshipId(ids[0], ids[1]);
                }
            });
        }
    }
}
