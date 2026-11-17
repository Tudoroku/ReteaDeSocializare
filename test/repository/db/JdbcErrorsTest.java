package repository.db;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/** Checks safe SQLState classification independently of database availability. */
class JdbcErrorsTest {
    @ParameterizedTest
    @CsvSource({
            "08001, Database connection unavailable",
            "08006, Database connection unavailable",
            "23502, Database constraint violation",
            "23503, Database constraint violation",
            "23505, Database constraint violation",
            "42601, Database error",
            ", Database error"
    })
    void classifiesWithoutExposingDriverDiagnostics(String state, String category) {
        SQLException cause = new SQLException("Sensitive driver diagnostics", state);
        var error = JdbcErrors.failure("saving user", cause);
        assertEquals(category + " while saving user", error.getMessage());
        assertSame(cause, error.getCause());
        assertEquals(state, ((SQLException) error.getCause()).getSQLState());
        assertFalse(error.getMessage().contains(cause.getMessage()));
    }
}
