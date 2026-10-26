import domain.Friendship;
import domain.FriendshipId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class FriendshipTest {

    @Test
    void testAutomaticCreationDate()
    {
        LocalDateTime before = LocalDateTime.now();
        Friendship friendship = new Friendship(2L, 1L);
        LocalDateTime after = LocalDateTime.now();

        assertEquals(new FriendshipId(1L, 2L), friendship.getId());
        assertNotNull(friendship.getCreatedAt());
        assertFalse(friendship.getCreatedAt().isBefore(before));
        assertFalse(friendship.getCreatedAt().isAfter(after));
    }

    @Test
    void testExplicitCreationDate()
    {
        LocalDateTime date = LocalDateTime.of(2024, 2, 29, 12, 30, 45, 123456789);
        Friendship friendship = new Friendship(2L, 1L, date);

        assertEquals(1L, friendship.getId().firstId());
        assertEquals(2L, friendship.getId().secondId());
        assertEquals(date, friendship.getCreatedAt());
    }

    @Test
    void testNullDateValidation()
    {
        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                new Friendship(1L, 2L, null);
            }
        });
    }

    @Test
    void testInvalidIdsInBothConstructors()
    {
        Long[][] invalidIds = {
                {null, 2L}, {1L, null}, {0L, 2L},
                {1L, 0L}, {-1L, 2L}, {1L, -2L}, {1L, 1L}
        };
        LocalDateTime date = LocalDateTime.of(2024, 1, 1, 10, 0);

        for(Long[] ids : invalidIds)
        {
            assertThrows(IllegalArgumentException.class, new Executable()
            {
                @Override
                public void execute()
                {
                    new Friendship(ids[0], ids[1]);
                }
            });
            assertThrows(IllegalArgumentException.class, new Executable()
            {
                @Override
                public void execute()
                {
                    new Friendship(ids[0], ids[1], date);
                }
            });
        }
    }

    @Test
    void testEqualsAndHashCodeIgnoreCreationDate()
    {
        LocalDateTime date = LocalDateTime.of(2024, 1, 1, 10, 0);
        Friendship first = new Friendship(1L, 2L, date);
        Friendship second = new Friendship(2L, 1L, date.plusYears(1));
        Friendship third = new Friendship(1L, 2L, date.minusYears(1));

        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(second, third);
        assertEquals(first, third);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new Friendship(1L, 3L, date));
        assertNotEquals(first, null);
        assertNotEquals(first, first.getId());

        Set<Friendship> friendships = new HashSet<>();
        friendships.add(first);
        friendships.add(second);
        assertEquals(1, friendships.size());
    }

    @Test
    void testToString()
    {
        LocalDateTime date = LocalDateTime.of(2024, 2, 29, 12, 30, 45);
        Friendship friendship = new Friendship(2L, 1L, date);

        assertEquals(
                "Friendship{firstId=1, secondId=2, createdAt=2024-02-29T12:30:45}",
                friendship.toString()
        );
    }
}
