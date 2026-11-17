# Java Social Network

A Java console application developed for **MAP (Advanced Programming Methods)**. Lab 5 adds PostgreSQL persistence to the existing university project, while preserving interchangeable memory and file repositories.

The application manages users and friendships, counts communities, and identifies the most sociable community by its longest simple path.

## Contents

- [Overview](#overview)
- [Technology stack](#technology-stack)
- [Architecture](#architecture)
- [Persistence modes](#persistence-modes)
- [Database schema](#database-schema)
- [Getting started](#getting-started)
- [Testing](#testing)
- [SQL injection protection](#sql-injection-protection)
- [Error handling](#error-handling)
- [Design decisions and limitations](#design-decisions-and-limitations)
- [Running a demonstration](#running-a-demonstration)
- [Earlier labs](#earlier-labs)

## Overview

The console supports:

- Adding and deleting users.
- Adding and deleting undirected friendships.
- Counting communities (connected components), including isolated users.
- Finding the community containing the longest simple path, measured in edges. A simple path never visits a user twice.

Community analysis builds an undirected graph from the repositories. Connected components use iterative depth-first traversal; longest paths use recursive search with backtracking. The most sociable community is determined by path length, rather than simply by its number of users. When communities tie, the first encountered is selected; traversal order is unspecified.

## Technology stack

| Tool | Version / role |
| --- | --- |
| Java | JDK 27; configured Gradle toolchain |
| Gradle | 9.8.0, through the included wrapper |
| PostgreSQL | Relational persistence; installed separately |
| PostgreSQL JDBC driver | 42.7.7 |
| JUnit Jupiter | 5.11.4 (JUnit 5) |
| IntelliJ IDEA | Optional IDE; import the Gradle project |

## Architecture

`Main` wires the configured repositories, validators, services, and console UI.

| Area | Responsibility |
| --- | --- |
| Domain | `User`, `Friendship`, and the normalized composite `FriendshipId` |
| Validation | Common user rules, friendship rules, and composable `Validator<T>` functions |
| Repositories | Generic `Repository<ID, E extends HasId<ID>>`, specialized user/friendship contracts, and MEMORY, FILE, DATABASE implementations |
| Repository selection | `RepositoryFactory` maps `RepositoryType` values to `Supplier` functions and constructs only the selected mode |
| Services | `UserService` coordinates validation and deletion cleanup; `FriendshipService` manages relationships |
| Community analysis | `CommunityService` derives graphs and computes components and longest paths |
| Console UI | `ConsoleUI` reads menu input and delegates to services |

### Project structure

Selected paths from the repository (Java sources use `src/`, and tests use `test/`):

```text
ReteaDeSocializare/
├── README.md
├── readme.txt
├── LAB5_TESTING.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
├── gradle/wrapper/
├── data/
│   ├── utilizatori.txt
│   └── prietenii.txt
├── src/
│   ├── Main.java
│   ├── domain/
│   ├── validation/
│   ├── repository/
│   │   ├── Repository.java
│   │   ├── RepositoryFactory.java
│   │   ├── RepositoryType.java
│   │   ├── AbstractFileRepository.java
│   │   ├── InMemoryRepository.java
│   │   ├── db/
│   │   │   ├── UserDbRepository.java
│   │   │   ├── FriendshipDbRepository.java
│   │   │   └── JdbcErrors.java
│   │   └── file/
│   │       ├── UserFileRepository.java
│   │       └── FriendshipFileRepository.java
│   ├── service/
│   ├── ui/
│   └── main/resources/config.example.properties
└── test/
    ├── UserDbRepositoryIntegrationTest.java
    ├── FriendshipDbRepositoryIntegrationTest.java
    └── JdbcTestSupport.java
```

### Repository contract

| Method | Result and meaning |
| --- | --- |
| `findOne(ID id)` | `Optional<E>` containing the matching entity, or empty if absent |
| `findAll()` | `Iterable<E>` of stored entities |
| `findAll(Predicate<E> rule)` | Default method returning a filtered `List<E>` using streams |
| `save(E entity)` | `void`; inserts without replacing an existing ID. DATABASE user storage assigns the generated ID to the supplied object |
| `delete(ID id)` | `Optional<E>` containing the deleted entity, or empty if absent |
| `update(E entity)` | Empty `Optional<E>` on success; the supplied entity when its ID is absent |

Validation and argument handling depend on the implementation. MEMORY repositories do not automatically apply domain validators; the services still enforce their rules. Repository update and retrieval methods are available in Java, but the console has no user-listing or user-update command.

## Persistence modes

| Mode | Storage | User IDs | Key behavior |
| --- | --- | --- | --- |
| `MEMORY` | In-memory entity references | Supplied explicitly | Data is lost when the process exits |
| `FILE` | UTF-8 text files | Supplied explicitly | Semicolon-separated records; names cannot contain semicolons, LF, or CR |
| `DATABASE` | PostgreSQL through JDBC | Generated by PostgreSQL | Prepared statements bind parameters; saved data survives application restarts |

Set the case-sensitive `repository.type` property to `MEMORY`, `FILE`, or `DATABASE`. Switching requires no source-code changes. **Restart the application after editing configuration**: the factory reads it once at startup. Only settings for the selected mode are required, although the local configuration file itself is always required.

FILE uses `users.file` and `friendships.file`. Each line represents either `id;firstName;lastName` or `id1;id2;ISO-LocalDateTime`. Loading trims fields, ignores blank rows, and reports and skips invalid rows. Missing files start empty and are created on the first successful save. Mutations rewrite the corresponding file.

## Database schema

The PostgreSQL database and schema must be provisioned separately. **The Java application does not create tables automatically**, and the repository supplies no migration or SQL setup file.

The following DDL documents the expected existing tables; it is not an application startup step:

```sql
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name VARCHAR NOT NULL,
    last_name VARCHAR NOT NULL
);

CREATE TABLE friendships (
    id1 BIGINT REFERENCES users(id) ON DELETE CASCADE,
    id2 BIGINT REFERENCES users(id) ON DELETE CASCADE,
    friends_from TIMESTAMP NOT NULL,
    PRIMARY KEY (id1, id2),
    CHECK (id1 < id2)
);
```

`FriendshipId` normalizes the endpoints into ascending order, so `(a, b)` and `(b, a)` identify the same friendship. The composite primary key prevents duplicate pairs; `CHECK (id1 < id2)` enforces the canonical order and excludes self-friendships. The foreign keys require both users to exist, and `ON DELETE CASCADE` removes related friendships when either user is deleted, including direct repository or database deletions.

User insertion omits the ID and uses `INSERT ... RETURNING id`. `UserDbRepository.save()` assigns that ID to the supplied `User`; `UserService.addUser()` returns it, and the console displays it.

## Getting started

### 1. Clone the Lab 5 branch

```powershell
git clone --branch lab5 https://github.com/Tudoroku/ReteaDeSocializare.git
Set-Location ReteaDeSocializare
```

On Unix/macOS, use `cd ReteaDeSocializare` after cloning.

### 2. Select Java 27

Install JDK 27 and configure `JAVA_HOME` and `PATH` to use it. Check the active Java version:

```powershell
java -version
.\gradlew.bat --version
```

The build requests a Java 27 toolchain. A separate Gradle installation is unnecessary; the wrapper downloads its configured distribution when needed. IntelliJ IDEA users can open the root Gradle project and select JDK 27 for Gradle and the project.

### 3. Prepare PostgreSQL for DATABASE mode

Ensure PostgreSQL is running and that the application database (for example, `socialnetwork`) already contains the [matching schema](#database-schema). Use a local database account with the permissions needed for table operations and identity generation. MEMORY and FILE do not require PostgreSQL.

### 4. Create the local configuration

Run from the repository root:

```powershell
Copy-Item src/main/resources/config.example.properties src/main/resources/config.properties
```

Unix/macOS equivalent:

```bash
cp src/main/resources/config.example.properties src/main/resources/config.properties
```

Copy only when creating the local file for the first time; preserve an existing local configuration. Edit it with your own settings:

```properties
repository.type=DATABASE

users.file=data/utilizatori.txt
friendships.file=data/prietenii.txt

db.url=jdbc:postgresql://localhost:5432/socialnetwork
db.user=<database_username>
db.password=<database_password>
```

Replace the placeholders locally. **`src/main/resources/config.properties` is Git-ignored and must remain uncommitted.** Do not put real credentials in documentation or JDBC URLs. DATABASE requires a nonblank URL and username and a present password property; an empty password is accepted if local authentication permits it.

### 5. Run the console

Keep the working directory at the repository root because configuration and relative data paths are resolved from there.

Windows PowerShell:

```powershell
.\gradlew.bat run --console=plain
```

Unix/macOS:

```bash
./gradlew run --console=plain
```

If the Unix wrapper lacks execute permission, run `chmod +x gradlew` first.

## Testing

Run the full suite from the repository root:

```powershell
.\gradlew.bat clean test --console=plain
```

Unix/macOS:

```bash
./gradlew clean test --console=plain
```

### PostgreSQL integration tests

Provision a **separate database named `socialnetwork_test`**, with the matching tables and constraints in its `public` schema. The integration tests use environment variables rather than application configuration:

| Variable | Value |
| --- | --- |
| `SOCIALNETWORK_TEST_DB_URL` | `jdbc:postgresql://localhost:5432/socialnetwork_test` |
| `SOCIALNETWORK_TEST_DB_USER` | Your dedicated test username |
| `SOCIALNETWORK_TEST_DB_PASSWORD` | Your test password |

Set these in the PowerShell process launching Gradle; the credentials below are placeholders:

```powershell
$env:SOCIALNETWORK_TEST_DB_URL = 'jdbc:postgresql://localhost:5432/socialnetwork_test'
$env:SOCIALNETWORK_TEST_DB_USER = '<test_username>'
$env:SOCIALNETWORK_TEST_DB_PASSWORD = '<test_password>'
.\gradlew.bat clean test --console=plain
```

Both integration classes have the `jdbc` tag and run as part of the normal test task. To rerun just those suites:

```powershell
.\gradlew.bat test --tests '*DbRepositoryIntegrationTest' --rerun-tasks --console=plain
```

The fixture skips integration tests when required variables are absent. Configured connection or schema errors fail tests. Safeguards in `JdbcTestSupport`:

- Refuse URLs pointing outside `socialnetwork_test`, reject URL properties and embedded credentials, and verify `current_database()`.
- Never recreate schemas, truncate tables, or reset sequences.
- Track test-created users and clean up only their rows and associated test friendships after each case. Identity sequences may advance; forced termination can prevent cleanup.

Use an isolated test database without concurrent writes.

### Recorded verification

- The earlier Codex environment run passed **98 non-database tests**; PostgreSQL tests were skipped because database settings were absent.
- A subsequent run on the user's local PostgreSQL installation successfully executed **21 integration tests**: 11 user-repository tests and 10 friendship-repository tests, with **0 failures and 0 skipped tests**. The current JUnit XML reports corroborate those integration results.

These are recorded results, not a claim that documentation changes reran the suite. Inspect `build/reports/tests/test/index.html` and `build/test-results/test/` after your own run. See [LAB5_TESTING.md](LAB5_TESTING.md) for coverage and verification details.

## SQL injection protection

JDBC repositories use `PreparedStatement` with placeholders and parameter binding (`setString`, `setLong`, and `setTimestamp`). SQL structure stays fixed while values are passed separately, so name text cannot become executable SQL syntax.

For example, this is a legitimate first name in DATABASE mode:

```text
'); DROP TABLE users; --
```

The production-validator integration test calls `UserService.addUser()` with the real `UserValidator`. It verifies a generated ID, exact storage and retrieval of this string, continued access to the `users` table, and successful creation and retrieval of a second normal user.

The following Java excerpt illustrates the parameter binding used for user insertion, inside a connection's try-with-resources scope:

```java
String sql = """
    INSERT INTO users (first_name, last_name)
    VALUES (?, ?)
    RETURNING id
    """;
try (PreparedStatement ps = c.prepareStatement(sql)) {
    ps.setString(1, entity.getFirstName());
    ps.setString(2, entity.getLastName());
    try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
            entity.setId(rs.getLong("id"));
        }
    }
}
```

FILE rejects the demonstration name because its semicolons conflict with the unescaped serialization format. This is a file-format restriction, not SQL injection protection: FILE mode does not execute SQL. Common validation permits this text in MEMORY and DATABASE.

## Error handling

JDBC `SQLException`s are wrapped in `RepositoryException`, preserving the original exception as the cause. `JdbcErrors` categorizes failures by SQLState:

| SQLState | User-facing category |
| --- | --- |
| Class `08` | Database connection unavailable |
| Class `23` | Database constraint violation |
| Other or missing state | Database error |

Messages include the failed operation without exposing raw driver diagnostics, credentials, or SQL values. During menu operations, `ConsoleUI` prints concise error messages without stack traces and continues the loop. Configuration and initial file-loading errors occur before that loop and can propagate at startup.

## Design decisions and limitations

- **IDs:** DATABASE requires a null initial user ID and assigns PostgreSQL's generated value; MEMORY and FILE require explicit IDs.
- **Deletion:** database cascades protect referential integrity. `UserService` also explicitly deletes friendships so cleanup works across all modes. These service-level repository calls do not share a transaction; a later failure does not roll back earlier deletions.
- **SQL binding:** prepared statements keep data separate from SQL and manage typed parameters. Connections, statements, and result sets use try-with-resources; repositories open connections per operation.
- **Validation composition:** `UserFileRepository` adds format checks with `validator.andThen(UserFileRepository::validateFileFormat)`, keeping common name rules independent of serialization.
- **Concurrent duplicates:** the friendship existence precheck and insertion use separate connections. A concurrent duplicate can therefore surface as a constraint error; the database primary key ultimately enforces uniqueness.
- **Scale and durability:** longest-simple-path search can be expensive on large graphs. FILE rewrites are not atomic; restoring memory after an I/O failure cannot restore partially written disk contents.

## Running a demonstration

### Console commands

| Option | Operation |
| ---: | --- |
| `1` | Add user; DATABASE displays a generated ID, FILE/MEMORY prompt for an ID |
| `2` | Delete user by ID |
| `3` | Add friendship using two user IDs; creation time is assigned automatically |
| `4` | Delete friendship using two user IDs |
| `5` | Display the number of communities |
| `6` | Display the most sociable community as user IDs, or report an empty network |
| `0` | Exit |

### Suggested workflow

1. Select DATABASE and start the console. Add at least three users with option `1` and record their displayed generated IDs.
2. Add friendships with option `3`, then use `5` and `6` to demonstrate community analysis. Use `4` and `2` to demonstrate relationship and user deletion with dedicated demonstration records.
3. Inspect saved rows in pgAdmin or psql, connected to the application database:

   ```sql
   SELECT id, first_name, last_name FROM users ORDER BY id;
   SELECT id1, id2, friends_from FROM friendships ORDER BY id1, id2;
   ```

4. Exit and restart. Run the PostgreSQL queries again and repeat community analysis to verify persistence. The console does not provide a listing command.
5. Change `repository.type` to FILE, restart, and demonstrate persistence in the configured files with explicit IDs. Switch to MEMORY, restart, add records, and restart again to show that those records disappear. Each mode uses its own storage; switching does not transfer records. Restore the intended mode afterward.
6. Return to DATABASE and add a user whose first name is exactly `'); DROP TABLE users; --`, with an ordinary last name. Inspect the row using the read-only queries above, confirm the exact text and intact table, and add another ordinary user. Enter the payload as console text only. FILE rejects the same name because of the delimiter. Clean up only your demonstration records.
7. To demonstrate graceful connection failure, exit and temporarily change the local `db.url` to a verified unused port, for example `jdbc:postgresql://127.0.0.1:1/socialnetwork`. Restart in DATABASE mode and choose `5`. Expect `Error: Database connection unavailable while retrieving users`, without a stack trace. Exit, restore the original URL, and restart. This simulates an unavailable endpoint; it does not stop the PostgreSQL server.

This workflow describes manual demonstration steps, not an automated or previously executed console session.

## Earlier labs

Earlier labs introduced file persistence, generic repositories, functional interfaces and validator composition, Stream API filtering, and community analysis. Lab 5 retains that structure and implements JDBC-backed PostgreSQL repositories. The original [readme.txt](readme.txt) remains available alongside this GitHub-facing guide.
