import domain.Friendship;
import domain.FriendshipId;
import domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import repository.InMemoryUserRepository;
import repository.file.FriendshipFileRepository;
import repository.file.UserFileRepository;
import service.CommunityService;
import service.FriendshipService;
import service.UserService;
import validation.FriendshipValidator;
import validation.UserValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class FriendshipFileRepositoryTest {
    @TempDir
    Path tempDir;

    private InMemoryUserRepository userRepo;
    private FriendshipValidator validator;
    private final LocalDateTime date = LocalDateTime.of(2024, 2, 29, 12, 30, 45, 123456789);

    @BeforeEach
    void setUp()
    {
        userRepo = new InMemoryUserRepository();
        for(long id = 1; id <= 4; id++)
        {
            userRepo.save(new User(id, "User" + id, "Popescu"));
        }
        validator = new FriendshipValidator(userRepo);
    }

    @Test
    void testMissingFileAndFirstSave() throws IOException
    {
        Path file = tempDir.resolve("nested/friendships.txt");
        var repo = new FriendshipFileRepository(file.toString(), validator);
        assertFalse(repo.findAll().iterator().hasNext());
        assertFalse(Files.exists(file));

        Friendship friendship = new Friendship(2L, 1L, date);
        repo.save(friendship);

        assertTrue(Files.isRegularFile(file));
        assertEquals(List.of("1;2;2024-02-29T12:30:45.123456789"), Files.readAllLines(file));
        assertSame(friendship, repo.findOne(new FriendshipId(1L, 2L)).orElseThrow());
    }

    @Test
    void testSaveAndRestartPreserveOriginalDates() throws IOException
    {
        Path file = tempDir.resolve("friendships.txt");
        var repo = new FriendshipFileRepository(file.toString(), validator);
        Friendship first = new Friendship(2L, 1L, date);
        Friendship second = new Friendship(4L, 3L, date.minusYears(2).withNano(0));
        repo.save(first);
        repo.save(second);
        List<String> lines = Files.readAllLines(file);

        assertEquals(2, lines.size());
        assertEquals(Set.of("1;2;" + first.getCreatedAt(), "3;4;" + second.getCreatedAt()),
                new HashSet<>(lines));
        var reloaded = new FriendshipFileRepository(file.toString(), validator);
        assertEquals(first.getId(), reloaded.findOne(first.getId()).orElseThrow().getId());
        assertEquals(date, reloaded.findOne(first.getId()).orElseThrow().getCreatedAt());
        assertEquals(second.getCreatedAt(),
                reloaded.findOne(second.getId()).orElseThrow().getCreatedAt());
        Set<FriendshipId> ids = new HashSet<>();
        for(Friendship friendship : reloaded.findAll())
        {
            ids.add(friendship.getId());
        }
        assertEquals(Set.of(first.getId(), second.getId()), ids);
        assertEquals(lines, Files.readAllLines(file));
    }

    @Test
    void testAutomaticallyCreatedDateSurvivesRestart()
    {
        Path file = tempDir.resolve("friendships.txt");
        var repo = new FriendshipFileRepository(file.toString(), validator);
        Friendship friendship = new Friendship(1L, 2L);
        LocalDateTime originalDate = friendship.getCreatedAt();
        repo.save(friendship);

        var reloaded = new FriendshipFileRepository(file.toString(), validator);
        assertEquals(originalDate, reloaded.findOne(friendship.getId()).orElseThrow().getCreatedAt());
    }

    @Test
    void testLoadExistingFriendshipsAndTrimFields() throws IOException
    {
        Path file = tempDir.resolve("friendships.txt");
        String contents = " 2 ; 1 ; " + date + " \n3;4;2020-01-01T10:00\n";
        Files.writeString(file, contents);

        var repo = new FriendshipFileRepository(file.toString(), validator);
        Friendship first = repo.findOne(new FriendshipId(1L, 2L)).orElseThrow();
        assertEquals(1L, first.getId().firstId());
        assertEquals(2L, first.getId().secondId());
        assertEquals(date, first.getCreatedAt());
        assertEquals(LocalDateTime.of(2020, 1, 1, 10, 0),
                repo.findOne(new FriendshipId(3L, 4L)).orElseThrow().getCreatedAt());
        assertEquals(contents, Files.readString(file));
    }

    @Test
    void testEmptyFileAndBlankLines() throws IOException
    {
        Path file = tempDir.resolve("friendships.txt");
        Files.createFile(file);
        var empty = new FriendshipFileRepository(file.toString(), validator);
        assertFalse(empty.findAll().iterator().hasNext());
        assertEquals(0L, Files.size(file));

        Files.writeString(file, "\n \t \n1;2;" + date + "\n\n3;4;" + date + "\n\t\n");
        var repo = new FriendshipFileRepository(file.toString(), validator);
        Set<FriendshipId> ids = new HashSet<>();
        for(Friendship friendship : repo.findAll())
        {
            ids.add(friendship.getId());
            assertEquals(date, friendship.getCreatedAt());
        }
        assertEquals(Set.of(new FriendshipId(1L, 2L), new FriendshipId(3L, 4L)), ids);
    }

    @Test
    void testCorruptedLinesDoNotPreventSubsequentFriendshipsLoading() throws IOException
    {
        Path file = tempDir.resolve("friendships.txt");
        String[] invalidLines = {
                "garbage", "1;2", "1;2;" + date + ";extra",
                "abc;2;" + date, "1;abc;" + date, ";2;" + date,
                "1;;" + date, "9223372036854775808;2;" + date,
                "0;2;" + date, "1;0;" + date, "-1;2;" + date,
                "1;-2;" + date, "1;1;" + date,
                "1;2;not-a-date", "1;2;2024-02-30T12:30", "1;2;2024-02-29",
                "1;2;", "1;2;null", "1;2;2024-02-29T25:00",
                "1;999;" + date, "999;2;" + date, "999;1000;" + date
        };
        List<String> lines = new ArrayList<>();
        Set<FriendshipId> expectedIds = new HashSet<>();
        long nextId = 10L;
        for(String invalidLine : invalidLines)
        {
            userRepo.save(new User(nextId, "Valid", "User"));
            lines.add(invalidLine);
            lines.add("1;" + nextId + ";" + date);
            expectedIds.add(new FriendshipId(1L, nextId));
            nextId++;
        }
        Files.write(file, lines);
        byte[] original = Files.readAllBytes(file);

        var repo = new FriendshipFileRepository(file.toString(), validator);
        Set<FriendshipId> actualIds = new HashSet<>();
        int count = 0;
        for(Friendship friendship : repo.findAll())
        {
            actualIds.add(friendship.getId());
            assertEquals(date, friendship.getCreatedAt());
            count++;
        }
        assertEquals(expectedIds, actualIds);
        assertEquals(expectedIds.size(), count);
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    @Test
    void testDuplicateReversedFriendshipsKeepFirstDateAndContinueLoading() throws IOException
    {
        Path file = tempDir.resolve("friendships.txt");
        List<String> lines = List.of("1;2;" + date,
                "2;1;" + date.plusYears(1), "3;4;" + date.minusYears(1));
        Files.write(file, lines);

        var repo = new FriendshipFileRepository(file.toString(), validator);
        assertEquals(date, repo.findOne(new FriendshipId(1L, 2L)).orElseThrow().getCreatedAt());
        assertEquals(date.minusYears(1),
                repo.findOne(new FriendshipId(3L, 4L)).orElseThrow().getCreatedAt());
        int count = 0;
        for(Friendship ignored : repo.findAll())
        {
            count++;
        }
        assertEquals(2, count);
        assertEquals(lines, Files.readAllLines(file));
    }

    @Test
    void testUpdatePersistsNewDateAndPreservesOtherFriendships() throws IOException
    {
        Path file = tempDir.resolve("friendships.txt");
        Files.write(file, List.of("1;2;" + date, "3;4;" + date));
        var repo = new FriendshipFileRepository(file.toString(), validator);
        Friendship replacement = new Friendship(2L, 1L, date.plusDays(1));
        repo.update(replacement);

        assertSame(replacement, repo.findOne(new FriendshipId(1L, 2L)).orElseThrow());
        List<String> lines = Files.readAllLines(file);
        assertEquals(2, lines.size());
        assertEquals(Set.of("1;2;" + replacement.getCreatedAt(), "3;4;" + date),
                new HashSet<>(lines));
        var reloaded = new FriendshipFileRepository(file.toString(), validator);
        assertEquals(replacement.getCreatedAt(),
                reloaded.findOne(new FriendshipId(1L, 2L)).orElseThrow().getCreatedAt());
        assertEquals(date, reloaded.findOne(new FriendshipId(3L, 4L)).orElseThrow().getCreatedAt());
    }

    @Test
    void testDeleteReversedIdAndDeleteLastFriendshipPersist() throws IOException
    {
        Path file = tempDir.resolve("friendships.txt");
        Files.write(file, List.of("1;2;" + date, "3;4;" + date));
        var repo = new FriendshipFileRepository(file.toString(), validator);
        repo.delete(new FriendshipId(2L, 1L));

        assertTrue(repo.findOne(new FriendshipId(1L, 2L)).isEmpty());
        assertEquals(List.of("3;4;" + date), Files.readAllLines(file));
        var reloaded = new FriendshipFileRepository(file.toString(), validator);
        assertTrue(reloaded.findOne(new FriendshipId(1L, 2L)).isEmpty());
        assertEquals(date, reloaded.findOne(new FriendshipId(3L, 4L)).orElseThrow().getCreatedAt());
        reloaded.delete(new FriendshipId(4L, 3L));
        assertEquals(0L, Files.size(file));
        assertFalse(new FriendshipFileRepository(file.toString(), validator)
                .findAll().iterator().hasNext());
    }

    @Test
    void testFailedCrudDoesNotChangePersistedFriendships() throws IOException
    {
        Path file = tempDir.resolve("friendships.txt");
        Files.write(file, List.of("1;2;" + date));
        var repo = new FriendshipFileRepository(file.toString(), validator);
        Friendship original = repo.findOne(new FriendshipId(1L, 2L)).orElseThrow();
        byte[] contents = Files.readAllBytes(file);
        Friendship[] invalidSaves = {
                null, new Friendship(2L, 1L, date.plusDays(1)),
                new Friendship(1L, 999L, date), new Friendship(999L, 2L, date),
                new Friendship(999L, 1000L, date)
        };
        for(Friendship friendship : invalidSaves)
        {
            assertThrows(IllegalArgumentException.class, new Executable()
            {
                @Override
                public void execute()
                {
                    repo.save(friendship);
                }
            });
            assertArrayEquals(contents, Files.readAllBytes(file));
            assertSame(original, repo.findOne(original.getId()).orElseThrow());
        }
        Friendship[] invalidUpdates = {
                null, new Friendship(1L, 999L, date), new Friendship(999L, 1000L, date),
                new Friendship(1L, 4L, date)
        };
        for(Friendship friendship : invalidUpdates)
        {
            assertThrows(IllegalArgumentException.class, new Executable()
            {
                @Override
                public void execute()
                {
                    repo.update(friendship);
                }
            });
            assertArrayEquals(contents, Files.readAllBytes(file));
            assertSame(original, repo.findOne(original.getId()).orElseThrow());
        }
        FriendshipId[] invalidIds = {null, new FriendshipId(1L, 4L)};
        for(FriendshipId id : invalidIds)
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
            assertSame(original, repo.findOne(original.getId()).orElseThrow());
        }
        var reloaded = new FriendshipFileRepository(file.toString(), validator);
        assertEquals(date, reloaded.findOne(original.getId()).orElseThrow().getCreatedAt());
        int count = 0;
        for(Friendship ignored : reloaded.findAll())
        {
            count++;
        }
        assertEquals(1, count);
    }

    @Test
    void testFailedValidationDoesNotCreateMissingFile()
    {
        Path file = tempDir.resolve("missing/friendships.txt");
        var repo = new FriendshipFileRepository(file.toString(), validator);
        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                repo.save(new Friendship(1L, 999L, date));
            }
        });
        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                repo.update(new Friendship(1L, 2L, date));
            }
        });
        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                repo.delete(new FriendshipId(1L, 2L));
            }
        });
        assertFalse(Files.exists(file));
        assertFalse(repo.findAll().iterator().hasNext());
    }

    @Test
    void testUpdateRejectsUserRemovedSinceLoading() throws IOException
    {
        Path file = tempDir.resolve("friendships.txt");
        Files.write(file, List.of("1;2;" + date));
        var repo = new FriendshipFileRepository(file.toString(), validator);
        Friendship original = repo.findOne(new FriendshipId(1L, 2L)).orElseThrow();
        byte[] contents = Files.readAllBytes(file);
        userRepo.delete(2L);

        assertThrows(IllegalArgumentException.class, new Executable()
        {
            @Override
            public void execute()
            {
                repo.update(new Friendship(1L, 2L, date.plusDays(1)));
            }
        });
        assertArrayEquals(contents, Files.readAllBytes(file));
        assertSame(original, repo.findOne(original.getId()).orElseThrow());
        assertEquals(date, original.getCreatedAt());
    }

    @Test
    void testUserDeletionPersistsFriendshipCleanupAndCommunities() throws IOException
    {
        Path usersFile = tempDir.resolve("users.txt");
        Path friendshipsFile = tempDir.resolve("friendships.txt");
        var users = new UserFileRepository(usersFile.toString(), new UserValidator());
        var friendships = new FriendshipFileRepository(friendshipsFile.toString(),
                new FriendshipValidator(users));
        var usersService = new UserService(users, new UserValidator(), friendships);
        var friendshipsService = new FriendshipService(users, friendships);
        for(long id = 1; id <= 4; id++)
        {
            usersService.addUser(new User(id, "User" + id, "Popescu"));
        }
        friendshipsService.addFriendship(1L, 2L);
        friendshipsService.addFriendship(1L, 3L);
        friendshipsService.addFriendship(2L, 3L);
        LocalDateTime survivingDate = friendships.findOne(new FriendshipId(2L, 3L))
                .orElseThrow().getCreatedAt();
        usersService.deleteUser(1L);

        assertEquals(List.of("2;3;" + survivingDate), Files.readAllLines(friendshipsFile));
        assertEquals(Set.of("2;User2;Popescu", "3;User3;Popescu", "4;User4;Popescu"),
                new HashSet<>(Files.readAllLines(usersFile)));
        var reloadedUsers = new UserFileRepository(usersFile.toString(), new UserValidator());
        var reloadedFriendships = new FriendshipFileRepository(friendshipsFile.toString(),
                new FriendshipValidator(reloadedUsers));
        assertTrue(reloadedUsers.findOne(1L).isEmpty());
        assertTrue(reloadedFriendships.findOne(new FriendshipId(1L, 2L)).isEmpty());
        assertTrue(reloadedFriendships.findOne(new FriendshipId(1L, 3L)).isEmpty());
        assertEquals(survivingDate, reloadedFriendships.findOne(new FriendshipId(2L, 3L))
                .orElseThrow().getCreatedAt());
        var communities = new CommunityService(reloadedUsers, reloadedFriendships);
        assertEquals(2, communities.getNumberOfCommunities());
        assertEquals(Set.of(Set.of(2L, 3L), Set.of(4L)),
                new HashSet<>(communities.getCommunities()));
        assertEquals(Set.of(2L, 3L), communities.getMostSociableCommunity());
    }
}
