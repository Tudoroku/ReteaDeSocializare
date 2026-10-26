import domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import repository.RepositoryException;
import repository.file.UserFileRepository;
import validation.UserValidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class UserFileRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void testMissingFileAndFirstSave() throws IOException
    {
        Path file = tempDir.resolve("nested/users.txt");
        var repo = new UserFileRepository(file.toString(), new UserValidator());

        assertFalse(repo.findAll().iterator().hasNext());
        assertTrue(repo.findOne(1L).isEmpty());
        assertFalse(Files.exists(file));

        User user = new User(1L, "Ana", "Popescu");
        repo.save(user);

        assertTrue(Files.isRegularFile(file));
        assertEquals(List.of("1;Ana;Popescu"), Files.readAllLines(file));
        assertSame(user, repo.findOne(1L).orElseThrow());
    }

    @Test
    void testSaveAndRestart() throws IOException
    {
        Path file = tempDir.resolve("users.txt");
        var repo = new UserFileRepository(file.toString(), new UserValidator());
        User first = new User(1L, "Ana Maria", "Popescu");
        User second = new User(2L, "Ștefan", "Ionescu-O'Neil");
        repo.save(first);
        repo.save(second);

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
        assertEquals(Set.of("1;Ana Maria;Popescu", "2;Ștefan;Ionescu-O'Neil"),
                new HashSet<>(lines));

        var reloaded = new UserFileRepository(file.toString(), new UserValidator());
        assertEquals(first.getFirstName(), reloaded.findOne(1L).orElseThrow().getFirstName());
        assertEquals(first.getLastName(), reloaded.findOne(1L).orElseThrow().getLastName());
        assertEquals(second.getFirstName(), reloaded.findOne(2L).orElseThrow().getFirstName());
        assertEquals(second.getLastName(), reloaded.findOne(2L).orElseThrow().getLastName());
        int count = 0;
        for(User ignored : reloaded.findAll())
        {
            count++;
        }
        assertEquals(2, count);
        assertEquals(lines, Files.readAllLines(file));
    }

    @Test
    void testLoadExistingUsersAndTrimFields() throws IOException
    {
        Path file = tempDir.resolve("users.txt");
        String contents = " 1 ; Ana ; Popescu \n2;Sebi;Ionescu\n";
        Files.writeString(file, contents);

        var repo = new UserFileRepository(file.toString(), new UserValidator());
        User first = repo.findOne(1L).orElseThrow();
        assertEquals(1L, first.getId());
        assertEquals("Ana", first.getFirstName());
        assertEquals("Popescu", first.getLastName());
        assertEquals("Sebi", repo.findOne(2L).orElseThrow().getFirstName());
        assertEquals("Ionescu", repo.findOne(2L).orElseThrow().getLastName());
        assertEquals(contents, Files.readString(file));
    }

    @Test
    void testEmptyFileAndBlankLines() throws IOException
    {
        Path file = tempDir.resolve("users.txt");
        Files.createFile(file);
        var empty = new UserFileRepository(file.toString(), new UserValidator());
        assertFalse(empty.findAll().iterator().hasNext());
        assertEquals(0L, Files.size(file));

        Files.writeString(file, "\n \t \n1;Ana;Popescu\n\n  \n2;Sebi;Ionescu\n\t\n");
        var repo = new UserFileRepository(file.toString(), new UserValidator());
        Set<Long> ids = new HashSet<>();
        for(User user : repo.findAll())
        {
            ids.add(user.getId());
        }
        assertEquals(Set.of(1L, 2L), ids);
    }

    @Test
    void testCorruptedLinesDoNotPreventSubsequentUsersLoading() throws IOException
    {
        Path file = tempDir.resolve("users.txt");
        String[] invalidLines = {
                "garbage", "2;Ana", "2;Ana;Popescu;extra", "abc;Ana;Popescu",
                ";Ana;Popescu", "9223372036854775808;Ana;Popescu",
                "0;Ana;Popescu", "-1;Ana;Popescu", "2;;Popescu",
                "2;   ;Popescu", "2;Ana;", "2;Ana;   ", "1;Changed;Duplicate"
        };
        List<String> lines = new ArrayList<>();
        lines.add("1;Ana;Popescu");
        Set<Long> expectedIds = new HashSet<>();
        expectedIds.add(1L);
        long nextId = 10L;
        for(String invalidLine : invalidLines)
        {
            lines.add(invalidLine);
            lines.add(nextId + ";Valid;User");
            expectedIds.add(nextId);
            nextId++;
        }
        Files.write(file, lines);
        byte[] original = Files.readAllBytes(file);

        var repo = new UserFileRepository(file.toString(), new UserValidator());
        Set<Long> actualIds = new HashSet<>();
        int count = 0;
        for(User user : repo.findAll())
        {
            actualIds.add(user.getId());
            if(user.getId() != 1L)
            {
                assertEquals("Valid", user.getFirstName());
                assertEquals("User", user.getLastName());
            }
            count++;
        }
        assertEquals(expectedIds, actualIds);
        assertEquals(expectedIds.size(), count);
        assertEquals("Ana", repo.findOne(1L).orElseThrow().getFirstName());
        assertEquals("Popescu", repo.findOne(1L).orElseThrow().getLastName());
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    @Test
    void testUpdatePersistsBothNames() throws IOException
    {
        Path file = tempDir.resolve("users.txt");
        Files.write(file, List.of("1;Ana;Popescu", "2;Chris;Georgescu"));
        var repo = new UserFileRepository(file.toString(), new UserValidator());
        User replacement = new User(1L, "Sebi", "Ionescu");
        repo.update(replacement);

        assertSame(replacement, repo.findOne(1L).orElseThrow());
        List<String> lines = Files.readAllLines(file);
        assertEquals(2, lines.size());
        assertEquals(Set.of("1;Sebi;Ionescu", "2;Chris;Georgescu"), new HashSet<>(lines));
        var reloaded = new UserFileRepository(file.toString(), new UserValidator());
        assertEquals("Sebi", reloaded.findOne(1L).orElseThrow().getFirstName());
        assertEquals("Ionescu", reloaded.findOne(1L).orElseThrow().getLastName());
        assertEquals("Chris", reloaded.findOne(2L).orElseThrow().getFirstName());
    }

    @Test
    void testDeleteAndDeleteLastUserPersist() throws IOException
    {
        Path file = tempDir.resolve("users.txt");
        Files.write(file, List.of("1;Ana;Popescu", "2;Sebi;Ionescu"));
        var repo = new UserFileRepository(file.toString(), new UserValidator());
        repo.delete(1L);

        assertTrue(repo.findOne(1L).isEmpty());
        assertEquals(List.of("2;Sebi;Ionescu"), Files.readAllLines(file));
        var reloaded = new UserFileRepository(file.toString(), new UserValidator());
        assertTrue(reloaded.findOne(1L).isEmpty());
        assertEquals("Sebi", reloaded.findOne(2L).orElseThrow().getFirstName());
        reloaded.delete(2L);
        assertEquals(0L, Files.size(file));
        assertFalse(new UserFileRepository(file.toString(), new UserValidator())
                .findAll().iterator().hasNext());
    }

    @Test
    void testFailedCrudDoesNotChangePersistedUsers() throws IOException
    {
        Path file = tempDir.resolve("users.txt");
        Files.write(file, List.of("1;Ana;Popescu"));
        var repo = new UserFileRepository(file.toString(), new UserValidator());
        User original = repo.findOne(1L).orElseThrow();
        byte[] contents = Files.readAllBytes(file);
        User[] invalidUsers = {
                null, new User(null, "Ana", "Popescu"), new User(0L, "Ana", "Popescu"),
                new User(-1L, "Ana", "Popescu"), new User(2L, null, "Popescu"),
                new User(2L, "", "Popescu"), new User(2L, " \t ", "Popescu"),
                new User(2L, "Ana", null), new User(2L, "Ana", ""),
                new User(2L, "Ana", " \t "), new User(1L, "Duplicate", "User")
        };
        for(User user : invalidUsers)
        {
            assertThrows(IllegalArgumentException.class, new Executable()
            {
                @Override
                public void execute()
                {
                    repo.save(user);
                }
            });
            assertArrayEquals(contents, Files.readAllBytes(file));
            assertSame(original, repo.findOne(1L).orElseThrow());
            assertTrue(repo.findOne(2L).isEmpty());
        }

        User[] invalidUpdates = {
                null, new User(null, "Ana", "Popescu"), new User(0L, "Ana", "Popescu"),
                new User(-1L, "Ana", "Popescu"), new User(2L, "Missing", "User"),
                new User(1L, null, "Popescu"), new User(1L, "", "Popescu"),
                new User(1L, " \t ", "Popescu"), new User(1L, "Ana", null),
                new User(1L, "Ana", ""), new User(1L, "Ana", " \t ")
        };
        for(User user : invalidUpdates)
        {
            assertThrows(IllegalArgumentException.class, new Executable()
            {
                @Override
                public void execute()
                {
                    repo.update(user);
                }
            });
            assertArrayEquals(contents, Files.readAllBytes(file));
            assertSame(original, repo.findOne(1L).orElseThrow());
        }
        Long[] invalidIds = {null, 0L, -1L, 99L};
        for(Long id : invalidIds)
        {
            assertThrows(IllegalArgumentException.class, new Executable()
            {
                @Override
                public void execute()
                {
                    repo.delete(id);
                }
            });
            assertArrayEquals(contents, Files.readAllBytes(file));
            assertSame(original, repo.findOne(1L).orElseThrow());
        }
        var reloaded = new UserFileRepository(file.toString(), new UserValidator());
        assertEquals("Ana", reloaded.findOne(1L).orElseThrow().getFirstName());
        assertEquals("Popescu", reloaded.findOne(1L).orElseThrow().getLastName());
    }

    @Test
    void testFailedCrudDoesNotCreateMissingFile()
    {
        Path file = tempDir.resolve("missing/users.txt");
        var repo = new UserFileRepository(file.toString(), new UserValidator());
        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                repo.save(new User(1L, "", "Popescu"));
            }
        });
        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                repo.update(new User(1L, "Ana", "Popescu"));
            }
        });
        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                repo.delete(1L);
            }
        });
        assertFalse(Files.exists(file));
        assertFalse(repo.findAll().iterator().hasNext());
    }

    @Test
    void testNullValidatorRejected()
    {
        Path file = tempDir.resolve("users.txt");
        assertThrows(NullPointerException.class, new Executable()
        {
            @Override
            public void execute()
            {
                new UserFileRepository(file.toString(), null);
            }
        });
        assertFalse(Files.exists(file));
    }

    @Test
    void testReadFailurePreservesCause() throws IOException
    {
        Path file = tempDir.resolve("directory.txt");
        Files.createDirectory(file);
        RepositoryException exception = assertThrows(RepositoryException.class, new Executable()
        {
            @Override
            public void execute()
            {
                new UserFileRepository(file.toString(), new UserValidator());
            }
        });
        assertInstanceOf(IOException.class, exception.getCause());
        assertTrue(exception.getMessage().contains(file.toString()));
        assertTrue(Files.isDirectory(file));
    }

    @Test
    void testFailedSaveDoesNotChangeMemory() throws IOException
    {
        Path parent = tempDir.resolve("blocked");
        Files.writeString(parent, "original contents");
        Path file = parent.resolve("users.txt");
        var repo = new UserFileRepository(file.toString(), new UserValidator());

        RepositoryException exception = assertThrows(RepositoryException.class, new Executable()
        {
            @Override
            public void execute()
            {
                repo.save(new User(1L, "Ana", "Popescu"));
            }
        });
        assertInstanceOf(IOException.class, exception.getCause());
        assertEquals("original contents", Files.readString(parent));
        assertFalse(Files.exists(file));
        assertTrue(repo.findOne(1L).isEmpty(), "A failed disk write must not insert a user in memory");
    }

    @Test
    void testFailedUpdateDoesNotChangeMemory() throws IOException
    {
        Path file = tempDir.resolve("users.txt");
        var repo = new UserFileRepository(file.toString(), new UserValidator());
        User original = new User(1L, "Ana", "Popescu");
        repo.save(original);
        // Replace this test's file with a directory to force a deterministic write failure.
        Files.delete(file);
        Files.createDirectory(file);

        assertThrows(RepositoryException.class, new Executable()
        {
            @Override
            public void execute()
            {
                repo.update(new User(1L, "Sebi", "Ionescu"));
            }
        });
        assertTrue(Files.isDirectory(file));
        assertSame(original, repo.findOne(1L).orElseThrow(),
                "A failed disk write must not replace the user in memory");
    }

    @Test
    void testFailedDeleteDoesNotChangeMemory() throws IOException
    {
        Path file = tempDir.resolve("users.txt");
        var repo = new UserFileRepository(file.toString(), new UserValidator());
        User original = new User(1L, "Ana", "Popescu");
        repo.save(original);
        Files.delete(file);
        Files.createDirectory(file);

        assertThrows(RepositoryException.class, new Executable()
        {
            @Override
            public void execute()
            {
                repo.delete(1L);
            }
        });
        assertTrue(Files.isDirectory(file));
        assertTrue(repo.findOne(1L).isPresent(),
                "A failed disk write must not delete the user from memory");
        assertSame(original, repo.findOne(1L).orElseThrow());
    }

    @Test
    void testNamesContainingSemicolonsAreRejected() throws IOException
    {
        Path file = tempDir.resolve("users.txt");
        var repo = new UserFileRepository(file.toString(), new UserValidator());
        User first = new User(1L, "Ana", "Popescu");
        User second = new User(2L, "Sebi", "Ionescu");
        repo.save(first);
        repo.save(second);
        var snapshot = repo.findAll();
        byte[] contents = Files.readAllBytes(file);

        User[] invalidUsers = {
                new User(3L, "Ana;Maria", "Popescu"),
                new User(4L, "Ana", "Popescu;Ionescu")
        };
        for(User user : invalidUsers)
        {
            assertThrows(IllegalArgumentException.class, new Executable()
            {
                @Override
                public void execute()
                {
                    repo.save(user);
                }
            });
            assertTrue(repo.findOne(user.getId()).isEmpty());
            assertEquals(snapshot, repo.findAll());
            assertSame(first, repo.findOne(1L).orElseThrow());
            assertSame(second, repo.findOne(2L).orElseThrow());
            assertArrayEquals(contents, Files.readAllBytes(file));
        }

        var reloaded = new UserFileRepository(file.toString(), new UserValidator());
        assertEquals(snapshot, reloaded.findAll());
        assertEquals(first.getFirstName(), reloaded.findOne(1L).orElseThrow().getFirstName());
        assertEquals(first.getLastName(), reloaded.findOne(1L).orElseThrow().getLastName());
        assertEquals(second.getFirstName(), reloaded.findOne(2L).orElseThrow().getFirstName());
        assertEquals(second.getLastName(), reloaded.findOne(2L).orElseThrow().getLastName());
        for(User user : invalidUsers)
        {
            assertTrue(reloaded.findOne(user.getId()).isEmpty());
        }
    }

    @Test
    void testNamesContainingLineBreaksAreRejected() throws IOException
    {
        Path file = tempDir.resolve("users.txt");
        var repo = new UserFileRepository(file.toString(), new UserValidator());
        User first = new User(1L, "Ana", "Popescu");
        User second = new User(2L, "Sebi", "Ionescu");
        repo.save(first);
        repo.save(second);
        var snapshot = repo.findAll();
        byte[] contents = Files.readAllBytes(file);

        User[] invalidUsers = {
                new User(3L, "Ana\nMaria", "Popescu"),
                new User(4L, "Ana", "Popescu\nIonescu"),
                new User(5L, "Ana\rMaria", "Popescu"),
                new User(6L, "Ana", "Popescu\rIonescu"),
                new User(7L, "Ana\r\nMaria", "Popescu"),
                new User(8L, "Ana", "Popescu\r\nIonescu")
        };
        for(User user : invalidUsers)
        {
            assertThrows(IllegalArgumentException.class, new Executable()
            {
                @Override
                public void execute()
                {
                    repo.save(user);
                }
            });
            assertTrue(repo.findOne(user.getId()).isEmpty());
            assertEquals(snapshot, repo.findAll());
            assertSame(first, repo.findOne(1L).orElseThrow());
            assertSame(second, repo.findOne(2L).orElseThrow());
            assertArrayEquals(contents, Files.readAllBytes(file));
        }

        var reloaded = new UserFileRepository(file.toString(), new UserValidator());
        assertEquals(snapshot, reloaded.findAll());
        assertEquals(first.getFirstName(), reloaded.findOne(1L).orElseThrow().getFirstName());
        assertEquals(first.getLastName(), reloaded.findOne(1L).orElseThrow().getLastName());
        assertEquals(second.getFirstName(), reloaded.findOne(2L).orElseThrow().getFirstName());
        assertEquals(second.getLastName(), reloaded.findOne(2L).orElseThrow().getLastName());
        for(User user : invalidUsers)
        {
            assertTrue(reloaded.findOne(user.getId()).isEmpty());
        }
    }
}
