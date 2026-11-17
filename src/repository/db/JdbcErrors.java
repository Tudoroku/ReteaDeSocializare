package repository.db;

import repository.RepositoryException;

import java.sql.SQLException;

/** Creates concise JDBC error messages without exposing driver diagnostics. */
final class JdbcErrors {
    private JdbcErrors() {
    }

    /**
     * Classifies a JDBC failure while preserving the original exception.
     * @param operation the operation being performed
     * @param cause the original JDBC failure
     * @return an exception with safe operation context and the original cause
     */
    static RepositoryException failure(String operation, SQLException cause) {
        String state = cause.getSQLState();
        String category = "Database error";
        if (state != null && state.startsWith("08")) {
            category = "Database connection unavailable";
        } else if (state != null && state.startsWith("23")) {
            category = "Database constraint violation";
        }
        return new RepositoryException(category + " while " + operation, cause);
    }
}
