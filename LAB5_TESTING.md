# Lab 5 test setup and submission review

Run the full suite from the project root with JDK 27:

```powershell
.\gradlew.bat clean test
```

## Isolated PostgreSQL tests

Provision `socialnetwork_test` separately with the intended Lab 5 schema already
installed. These tests never create databases, tables, constraints, or schemas.
They expect public tables with:

- `users(id, first_name, last_name)`: database-generated positive IDs and NOT NULL names.
- `friendships(id1, id2, friends_from)`: a unique composite pair, timestamp without
  time zone, and foreign keys to users. The direct repository deletion test expects
  ON DELETE CASCADE on both endpoints; the service cleanup test checks the separate
  application cleanup path.

Set these environment variables in the shell launching Gradle:

- `SOCIALNETWORK_TEST_DB_URL`: `jdbc:postgresql://localhost:5432/socialnetwork_test`
- `SOCIALNETWORK_TEST_DB_USER`: a test database login with table and sequence access.
- `SOCIALNETWORK_TEST_DB_PASSWORD`: supply securely through your environment; an
  explicitly set empty value is allowed when your authentication setup permits it.

Use a dedicated test login restricted to `socialnetwork_test` where possible.
Do not put passwords in source, application config, the JDBC URL, or this document.
The test fixture does not read application configuration. It rejects URLs other
than `jdbc:postgresql://host[:port]/socialnetwork_test`, disallows URL properties,
and verifies `current_database()` before executing repository operations.

If any required setting is absent, each PostgreSQL integration test is skipped
with an explicit JUnit assumption message. When settings are present, unsafe URLs,
connection failures, missing tables, and incorrect constraints fail tests rather
than silently skipping them. Inspect `build/reports/tests/test/index.html` or the
XML in `build/test-results/test` for individual results and skip reasons.

The two `*IntegrationTest` classes are tagged `jdbc` and are included in the
ordinary test task. To run just those classes after configuration:

```powershell
.\gradlew.bat test --tests '*DbRepositoryIntegrationTest' --rerun-tasks
```

Cleanup tracks generated user IDs per test and deletes only their friendships
and users in an `@AfterEach` transaction, including after assertion failures.
There is no TRUNCATE, sequence reset, global DELETE, or schema change. Assertions
do not assume an empty database or particular generated IDs. Sequences naturally
advance, so gaps are expected. Run against a dedicated test database without
concurrent application writes. Forced process termination can prevent teardown.

`JdbcRepositoryContractTest` runs without configuration: it checks argument
validation and uses real JDBC connections to loopback port 1 with short timeouts
to check SQL exception wrapping. No JDBC mocks or substitute persistence are used.

For database constraint and injection parameter-binding diagnostics only, selected
tests supply a no-op validator so the operation actually reaches PostgreSQL.
Other integration tests use the production validators. These diagnostics do not
prove that the normal application accepts the injection string.

## Required changes, reported without changing production behavior

1. **SQL injection example is blocked by validation.** The exact name
   `'); DROP TABLE users; --` contains semicolons, rejected by `UserValidator`.
   `Main` supplies that validator to both `UserService` and `UserDbRepository`,
   so changing only the repository's validator would not fix the application.
   JDBC uses parameter binding (`setString`), which is the correct protection.
   Keep common name/ID validation and introduce persistence-specific validation:
   keep semicolon/line-break restrictions for FILE, and allow the required text
   for DATABASE in both service and repository wiring. Preserve existing FILE
   tests. This needs a deliberate production change; none was made here.
   The contract test records today's rejection, and an isolated integration test
   checks exact text round-trip when the validator is bypassed diagnostically.

2. **User-facing SQL diagnostics lack detail.** All JDBC catch blocks preserve
   the original SQLException as the direct cause. User repository messages
   identify the operation (and sometimes ID) but omit the SQL explanation;
   `ConsoleUI` displays only `getMessage()`. A connection problem and a constraint
   violation therefore look similar to the user. Friendship messages include the
   driver explanation, although save can report a lookup error because of its
   duplicate precheck. Consider consistent, useful operation/SQLState context
   while preserving causes and keeping credentials out of messages.

3. **Concurrent duplicate handling has a race.** Friendship save checks existence
   on one connection and inserts on another. Sequential duplicates produce the
   documented IllegalArgumentException, but a concurrent insert can instead
   produce RepositoryException with SQLState 23505. The database must enforce
   composite uniqueness. A future targeted fix can translate that SQLState into
   the documented duplicate rejection. No concurrency behavior was changed.

4. **Direct user cleanup depends on the database schema.** UserDbRepository only
   deletes the user; repository-level friendship cleanup requires FK cascades.
   UserService instead deletes friendships first, with no shared transaction.
   A later failure can leave partial service-level deletion. Tests exercise both
   paths; the actual schema/cascade behavior remains unverified until the test
   database is configured. No existing schema was inspected or modified.

5. **Submission documentation is stale.** UserDbRepository Javadoc, repository
   factory/interface comments, generated docs, and readme.txt still describe JDBC
   placeholders/Lab 4 in places. Update these for the final submission.
   UserDbRepository also accepts null constructor dependencies, unlike
   FriendshipDbRepository, so a null validator can later cause an unhelpful NPE.

## Verification in this workspace

`.\gradlew.bat clean test` completed successfully. The existing 75 tests and
14 new database-independent contract test cases passed: **89 passed, 0 failed,
0 skipped**. PostgreSQL integration tests: **0 passed, 0 failed, 20 skipped**
because the test database environment variables were absent.

Remaining before submission: configure the isolated database and run all 20 JDBC
integration tests; resolve the production validation/wiring blocker and verify
the exact injection name through the normal application path; update stale Lab 4
documentation. The supplied requirements were reviewed; no separate complete Lab 5
assignment specification or schema DDL was present to audit further requirements.

No production code, existing tests, Gradle configuration, application data,
database schemas, or .idea files were changed by this work. No commit or push was made.
