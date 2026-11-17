import domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import repository.InMemoryRepository;
import repository.InMemoryUserRepository;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

public class InMemoryRepositoryTest {

    @Test
    void testMutationsPreserveUnrelatedEntities()
    {
        var repo = new InMemoryRepository<Long, User>();
        User first = new User(1L, "Ana", "Popescu");
        User second = new User(2L, "Sebi", "Ionescu");
        repo.save(first);
        repo.save(second);

        User missing = new User(3L, "Missing", "User");
        assertSame(missing, repo.update(missing).orElseThrow());
        assertTrue(repo.delete(3L).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> repo.delete(null));
        assertSame(first, repo.findOne(1L).orElseThrow());
        assertSame(second, repo.findOne(2L).orElseThrow());

        User replacement = new User(1L, "Updated", "User");
        assertTrue(repo.update(replacement).isEmpty());
        assertSame(second, repo.findOne(2L).orElseThrow());
        assertSame(replacement, repo.delete(1L).orElseThrow());
        assertSame(second, repo.findOne(2L).orElseThrow());
        assertEquals(java.util.List.of(second), repo.findAll());
    }

    @Test
    void testUserRepositoryRejectsNullIdWithoutChangingUsers()
    {
        var repo = new InMemoryUserRepository();
        User original = new User(1L, "Ana", "Popescu");
        repo.save(original);
        var snapshot = repo.findAll();
        User user = new User(null, "Sebi", "Ionescu");

        assertFalse(repo.generatesIds());
        assertThrows(IllegalArgumentException.class, () -> repo.save(user));
        assertEquals(snapshot, repo.findAll());
        assertThrows(IllegalArgumentException.class, () -> repo.update(user));
        assertEquals(snapshot, repo.findAll());
        assertSame(original, repo.findOne(1L).orElseThrow());
        assertTrue(repo.findOne(null).isEmpty());
        assertNull(user.getId());
    }

    @Test
    void testSave()
    {
        //Test save + find entity
        var repo = new InMemoryRepository<Long, User>();
        User user = new User(
                1L,
                "Ana",
                "Popescu"
        );
        repo.save(user);
        assertTrue(repo.findOne(1L).isPresent());
        assertSame(user, repo.findOne(1L).orElseThrow());


        //Test save duplicate ID
        User user1 = new User(
                1L,
                "Gheorghe",
                "Popescu"
        );

        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                repo.save(user1);
            }
        });

        //Test save null entity
        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                repo.save(null);
            }
        });

        //Test save null id
        User nullUser = new User(
                null,
                "null",
                "Popescu"
        );
        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                repo.save(nullUser);
            }
        });
    }

    @Test
    void testFindOne()
    {
        //Test findOne on invalid ID
        var repo = new InMemoryRepository<Long, User>();
        assertTrue(repo.findOne(21L).isEmpty());

        //Test findOne on valid ID
        User user1 = new User(
                21L,
                "Gheorghe",
                "Popescu"
        );
        repo.save(user1);
        assertSame(user1, repo.findOne(21L).orElseThrow());


    }

    @Test
    void testDelete()
    {
        //Test delete on valid ID
        var repo = new InMemoryRepository<Long, User>();
        User user = new User(
                1L,
                "Ana",
                "Popescu"
        );
        repo.save(user);

        assertSame(user, repo.delete(1L).orElseThrow());

        assertFalse(repo.findAll().iterator().hasNext());

        //Test delete on invalid ID
        assertTrue(repo.delete(1L).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> repo.delete(null));
    }

    @Test
    void testUpdate()
    {
        //Test update on valid existing entity
        var repo = new InMemoryRepository<Long, User>();
        User user = new User(
                1L,
                "Ana",
                "Popescu"
        );

        repo.save(user);

        User user1 = new User(
                1L,
                "Sebi",
                "Ionescu"
        );

        assertTrue(repo.update(user1).isEmpty());
        assertSame(user1, repo.findOne(1L).orElseThrow());
        assertEquals("Sebi", repo.findOne(1L).orElseThrow().getFirstName());
        assertEquals("Ionescu", repo.findOne(1L).orElseThrow().getLastName());


        //Test update on inexistent entity
        User user2 = new User(
                3L,
                "Parizer",
                "Popescu"
        );

        assertSame(user2, repo.update(user2).orElseThrow());
        assertTrue(repo.findOne(3L).isEmpty());
        assertSame(user1, repo.findOne(1L).orElseThrow());

        //Test update on null entity
        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                repo.update(null);
            }
        });

        //Test update on null ID
        User nullUser = new User(
                null,
                "null",
                "Popescu"
        );
        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                repo.update(nullUser);
            }
        });
    }

    @Test
    void testFindAll() {
        var repo = new InMemoryRepository<Long, User>();

        User user1 = new User(1L, "Ana", "Popescu");
        User user2 = new User(2L, "Sebi", "Popescu");
        User user3 = new User(3L, "Parizer", "Popescu");

        // Repository initial gol
        assertFalse(repo.findAll().iterator().hasNext());

        // Salvare utilizatori
        repo.save(user1);
        repo.save(user2);
        repo.save(user3);

        // Colectatre rezultatele intr un Set
        Set<User> actualUsers = new HashSet<>();

        for (User user : repo.findAll()) {
            actualUsers.add(user);
        }

        Set<User> expectedUsers = Set.of(user1, user2, user3);

        assertEquals(expectedUsers, actualUsers);
        assertEquals(3, actualUsers.size());

        // Stergerea tuturor utilizatorilor
        repo.delete(1L);
        repo.delete(2L);
        repo.delete(3L);

        assertFalse(repo.findAll().iterator().hasNext());

        //Test lista imutabila
        repo.save(user1);
        Iterator<User> it = repo.findAll().iterator();
        it.next();

        assertThrows(UnsupportedOperationException.class, new Executable()
        {
            @Override
            public void execute()
            {
                it.remove();
            }
        });

    }

}
