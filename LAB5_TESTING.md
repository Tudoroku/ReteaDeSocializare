# Lab 5 testing and submission review

The requirements reproduced in the task prompt are the specification for this
completion. No assignment PDF was available or consulted.

Run the full suite from the project root with JDK 27:

```powershell
.\gradlew.bat clean test --console=plain
```

## Isolated PostgreSQL tests

Provision `socialnetwork_test` separately with the existing Lab 5 schema already
installed. It is different from the application database `socialnetwork`.
Tests never create databases, tables, constraints, or schemas. Expected tables:

- `users`: `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`, non-null
  `first_name` and `last_name` VARCHAR columns.
- `friendships`: BIGINT `id1` and `id2`, non-null `friends_from TIMESTAMP`,
  primary key `(id1, id2)`, `CHECK (id1 < id2)`, and foreign keys referencing
  `users(id)` with `ON DELETE CASCADE` on both endpoints.

Supply these environment variables securely in the shell launching Gradle:

- `SOCIALNETWORK_TEST_DB_URL=jdbc:postgresql://localhost:5432/socialnetwork_test`
- `SOCIALNETWORK_TEST_DB_USER=<test username>`
- `SOCIALNETWORK_TEST_DB_PASSWORD=<test password>`

Use a dedicated test login with table/sequence access, restricted to the test
database where possible. An explicitly present empty password is accepted when
local authentication permits it. Never write real credentials to documentation,
logs, source code, or JDBC URLs. Local application config remains Git-ignored.

`JdbcTestSupport` does not read application configuration. It accepts only
`jdbc:postgresql://host[:port]/socialnetwork_test`, disallows URL properties and
credentials, and verifies `current_database()` before repository operations.
Missing settings skip each integration test with an explicit JUnit assumption.
Configured unsafe URLs, connection failures, or incorrect schemas fail tests
rather than silently skipping them.

Cleanup tracks generated user IDs before save and deletes only their friendships
and users in an `@AfterEach` transaction, including after assertion failures.
There is no TRUNCATE, global DELETE, sequence reset, or schema modification.
Existing data is preserved; sequences may advance. Tests do not assume an empty
database or particular IDs. Use an isolated database without concurrent writes.
Forced termination can prevent teardown. The fixture safeguards were unchanged.

Both integration classes carry the `jdbc` tag and run with the normal test task.
To rerun only them after configuration:

```powershell
.\gradlew.bat test --tests '*DbRepositoryIntegrationTest' --rerun-tasks --console=plain
```

Inspect `build/reports/tests/test/index.html` and the JUnit XML files in
`build/test-results/test` for results and skip reasons.

## Regression coverage and implementation decisions

- The injection regression now calls `UserService.addUser()` with the real
  `UserValidator`, then `UserDbRepository.save()` with the same production
  validator rules. It stores the exact first name `'); DROP TABLE users; --`,
  checks a positive generated ID and exact retrieval, directly queries users to
  verify table accessibility, and saves/retrieves a second normal user through
  the service. Only tracked rows are cleaned up.
- Positive DATABASE save/update cases cover semicolons and LF/CR in both names.
  Invalid-name cases retain null, empty and whitespace-only names. Only constraint
  diagnostics intentionally bypass validation to reach actual NOT NULL/FK checks;
  the injection regression does not bypass it.
- Existing FILE delimiter tests now check rejected updates as well as saves,
  unchanged stored users and file bytes, and successful reload. Common validation
  remains independent of the FILE format. A MEMORY service regression confirms
  names containing FILE delimiters still pass the production validator.
- Disconnected JDBC tests retain null-argument and connection-error checks. The
  injection string passes production validation and reaches JDBC, where the
  deliberately unavailable endpoint produces a connection error instead.
- Shared JDBC error classification maps SQLState class 08 to connection
  unavailable, class 23 to constraint violation, and other/null states to a
  database error. Messages identify the operation without raw driver diagnostics.
  Tests preserve SQLState checks and original causes, including FK SQLState 23503.
  Additional database-independent tests verify safe messages and cause identity.
- JDBC CRUD, Repository signatures, Supplier-based factory, schema, cascade
  deletion and explicit service cleanup remain intact. Service cleanup still
  spans multiple independent repository calls without a shared transaction.
  The friendship duplicate precheck concurrency race remains unchanged.

## Historical results (do not treat as this run)

The earlier recorded run had **89 passed, 0 failed, 20 skipped** because database
settings were missing. The old wording separated 89 database-independent passes
from 20 JDBC skips; it was not a fully executed integration run.

Subsequently, the user reported successful real PostgreSQL runs of the previous
integration suites: **UserDbRepositoryIntegrationTest: 10 executed, 0 skipped,
0 failed** and **FriendshipDbRepositoryIntegrationTest: 10 executed, 0 skipped,
0 failed**. Those establish a prior working environment, but do not verify the
modified regressions in the current checkout.

## Current verification: 2026-11-18

Executed `.\gradlew.bat clean test --console=plain` with **BUILD SUCCESSFUL**.
JUnit XML totals: **119 tests, 98 passed, 0 failed, 21 skipped**.

| Suite | Passed | Failed | Skipped |
| --- | ---: | ---: | ---: |
| UserDbRepositoryIntegrationTest | 0 | 0 | 11 |
| FriendshipDbRepositoryIntegrationTest | 0 | 0 | 10 |
| All other suites | 98 | 0 | 0 |

The current process had none of the three required test database environment
variables. All 21 integration cases were skipped by the fixture assumption.
**NOT VERIFIED in this run:** real PostgreSQL execution of the modified injection
regression, DATABASE delimiter round trips, generated IDs, CRUD, FK constraints,
and cascades. Configure the isolated database and rerun both suites to verify.
No production database or application data was accessed. No service was stopped.

The 98 passing cases include all 19 UserFileRepositoryTest cases, all 15
JdbcRepositoryContractTest cases, all seven SQLState helper cases, and the MEMORY
regression. Connection failures are exercised using actual JDBC against loopback
port 1; no real database or credentials are needed for those cases.

Manual pgAdmin CRUD, console generated-ID display, restart persistence, mode
switching and actual stopped-server demonstrations were **NOT VERIFIED** in this
run. Procedures are in readme.txt; no unperformed demonstration is claimed.
There are no intentional divergences from the supplied Lab 5 requirements.
No commit or push was performed; pre-existing .idea changes were left untouched.
