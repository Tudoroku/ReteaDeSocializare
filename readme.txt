MAP LAB 5 - SOCIAL NETWORK

PURPOSE AND EXISTING FUNCTIONALITY

Lab 5 continues the Lab 4 application with working PostgreSQL persistence.
Existing functionality remains: add/delete users and friendships, count
communities, and find the most sociable community. A community is a connected
component; an isolated user forms one community. Sociability is measured by
the longest simple path (number of edges).

BUILD AND CONFIGURATION

Use JDK 27 and the included Gradle wrapper (9.8.0). The project uses Java
sources in src/, JUnit tests in test/, and PostgreSQL JDBC driver 42.7.7.
Open the Gradle project in IntelliJ or run from the project root:

.\gradlew.bat run
.\gradlew.bat clean test

Copy src/main/resources/config.example.properties to the local
src/main/resources/config.properties, then supply your own settings.
The real config.properties is Git-ignored and must never be committed:
it can contain real database credentials. The example contains placeholders.

Switch persistence exclusively through repository.type=MEMORY, FILE, or
DATABASE. Restart the application after changing configuration because the
factory reads it once. No source-code changes are needed to switch modes.

MEMORY: entity references live only until exit.
FILE: users.file and friendships.file select UTF-8 data files.
DATABASE: db.url, db.user and db.password supply JDBC connection settings.
For the application, db.url=jdbc:postgresql://localhost:5432/socialnetwork.
URL and username must be nonblank; the password setting must be present.
An empty password is accepted if the local authentication setup permits it.

DATABASE DESIGN (EXISTING SCHEMA; NOT AUTOMATICALLY CREATED)

Database: socialnetwork

users:
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
  first_name VARCHAR NOT NULL
  last_name VARCHAR NOT NULL

friendships:
  id1 BIGINT REFERENCES users(id) ON DELETE CASCADE
  id2 BIGINT REFERENCES users(id) ON DELETE CASCADE
  friends_from TIMESTAMP NOT NULL
  PRIMARY KEY (id1, id2)
  CHECK (id1 < id2)

FriendshipId normalizes both endpoints into ascending order. The composite
primary key prevents duplicate pairs, while the check prevents reversed or
self pairs. Both foreign keys require existing users. ON DELETE CASCADE
removes friendships when either endpoint is deleted, including direct database
or repository deletion; it prevents orphan relationships independently of UI
or service callers. The existing schema is preserved.

UserService also explicitly deletes a user's friendships before deleting the
user. This preserves cleanup for MEMORY and FILE as well as DATABASE.
Known limitation: these repository calls do not share one transaction.
If a later call fails, earlier completed deletions are not rolled back.
The existing friendship duplicate precheck uses a separate connection from
insertion; concurrent duplicates can therefore surface as a database constraint
error. The primary key remains the final enforcement of uniqueness.

JDBC IMPLEMENTATION AND REPOSITORY CONTRACT

UserDbRepository and FriendshipDbRepository implement the existing interfaces.
Each operation uses DriverManager.getConnection(url, user, password) with
settings supplied by RepositoryFactory. Connections, PreparedStatements and
ResultSets use try-with-resources. User input is bound to SQL placeholders
with setString, setLong and setTimestamp, never concatenated into SQL.

User inserts omit id and use INSERT ... RETURNING id. PostgreSQL generates
the ID, save assigns it to the supplied User, UserService.addUser returns it,
and ConsoleUI displays it. DATABASE user insertion requires a null initial ID;
MEMORY and FILE use explicit IDs.

The contract remains:
  Optional<E> findOne(ID): matching entity or empty when absent.
  Iterable<E> findAll(): stored entities.
  void save(E): validate and insert; DATABASE user save assigns its ID.
  Optional<E> update(E): empty on success, the supplied entity when absent.
  Optional<E> delete(ID): the deleted entity, or empty when absent.

JDBC operations reject null arguments with IllegalArgumentException and
validate before save/update. Constructor dependencies must be non-null.
SQLExceptions are wrapped in RepositoryException with the original exception
as the direct cause. SQLState class 08 produces a connection-unavailable
message; class 23 produces a constraint-violation message; other states produce
a database-error message. Each includes the failed operation. Driver diagnostics,
credentials and SQL values are omitted from these user-facing messages.
ConsoleUI prints the exception message without a stack trace.

VALIDATION AND FILE PERSISTENCE

UserValidator checks common rules: the user object must be non-null, a supplied
ID must be positive, and both names must be non-null and nonblank. A null ID is permitted
for database-generated IDs. Semicolons and embedded line breaks are valid
common names in DATABASE and MEMORY.

UserFileRepository composes the supplied validator with validateFileFormat via
validator.andThen(UserFileRepository::validateFileFormat). FILE rejects ';',
LF and CR in either name before save/update, preserving the unescaped format:
  data/utilizatori.txt: id;firstName;lastName
  data/prietenii.txt: id1;id2;ISO-LocalDateTime

AbstractFileRepository uses Template Method: subclasses parse/format rows;
the base class loads and rewrites files. Loaded fields are trimmed. Missing
files start empty and are created on first save. Blank rows are ignored;
invalid rows are reported with their line number and skipped. Failed writes
restore the previous in-memory state, but a file rewrite is not atomic and
may leave a partially written file. Rejected validation leaves file bytes
unchanged. FILE and DATABASE retain saved data across application restarts;
MEMORY does not.

PRESERVED LAB 4 ARCHITECTURE

Services depend on UserRepository and FriendshipRepository. RepositoryFactory
maps each RepositoryType to a Supplier and constructs only the selected mode;
settings for unused modes are not required. MEMORY repositories do not perform
automatic domain validation; services still apply their validators.

Validator<T> supports andThen composition. Predicate-based repository queries
and UserCriteria compose filters using and, or, negate and Predicate.not.
Function and method references map users to IDs and entities to file rows;
Consumer/forEach performs friendship cleanup. CommunityService uses streams
to build graphs and select components, retaining iterative DFS stack control
and explicit backtracking for longest paths. The console retains its menu loop.
The earlier --demo argument runs the Lab 4 functional-interface demonstration
in memory, including its extra first-name length rule; that rule is not a new
Lab 5 requirement or part of the normal console flow.

TEST DATABASE AND VERIFICATION

Integration tests require a separately provisioned socialnetwork_test database
with the same existing schema and both ON DELETE CASCADE foreign keys. It is
separate from the application database socialnetwork. Supply these variables
securely in the process launching Gradle:

SOCIALNETWORK_TEST_DB_URL=jdbc:postgresql://localhost:5432/socialnetwork_test
SOCIALNETWORK_TEST_DB_USER=<test username>
SOCIALNETWORK_TEST_DB_PASSWORD=<test password>

Never put real credentials in documentation or logs. JdbcTestSupport does not
read application configuration. It accepts only the test database URL, checks
current_database(), never changes schemas, never uses TRUNCATE or global DELETE,
and cleans only friendships/users belonging to tracked test users. Existing
data is preserved; identity sequences can advance. Missing settings skip JDBC
integration tests explicitly; configured connection/schema problems fail them.
See LAB5_TESTING.md and build/test-results/test XML for verified results.

LAB 5 DEMONSTRATION PROCEDURE (MANUAL, NOT A CLAIM OF EXECUTION)

1. Select DATABASE in local configuration. Run the console and add a user.
   Record the displayed positive PostgreSQL-generated ID. In pgAdmin, inspect
   that row and exercise repository CRUD through integration tests against the
   isolated test database (the console exposes the existing Lab 2 commands).
   Verify user name updates, friendship dates and delete return behavior there.
2. Add users and friendships with the console; inspect the application rows in
   pgAdmin. Delete only demonstration users and verify their friendships vanish.
3. Exit and restart the application; list the created users and friendships to
   demonstrate persistence. Clean up only demonstration rows when finished.
4. Change repository.type to FILE, restart and demonstrate file persistence;
   switch to MEMORY, restart and demonstrate that data disappears on exit.
   Restore the intended mode when finished.
5. To demonstrate an unavailable server without stopping any service, temporarily
   set db.url in local configuration to a verified unused local port (for example
   jdbc:postgresql://127.0.0.1:1/socialnetwork), run DATABASE mode and request users.
   Expect 'Database connection unavailable while retrieving users', without a
   stack trace. Restore the original URL. This simulates an unavailable endpoint;
   an actual stopped-server demonstration is manual: if needed, an operator may
   temporarily stop and then restart their own test server and repeat the query.
   No script or automated test stops PostgreSQL.
6. In DATABASE mode enter exactly this first name as ordinary console text:
   '); DROP TABLE users; --
   Use a normal last name. Record its generated ID, retrieve the exact name,
   confirm users remains accessible in pgAdmin, and add/retrieve another normal
   user. PreparedStatement binding treats the whole name as data. Do not execute
   the payload as SQL in pgAdmin. Remove only the users created for this demo.
   FILE must reject that name because of its delimiters.

DELIVERABLES

Java sources, JUnit tests, Gradle/IntelliJ project, config.example.properties,
this readme and Git history. Local config.properties and its credentials are
excluded from version control. No database migration or new framework is needed.
