package repository;

/** Reports a repository or configuration I/O failure while preserving its cause. */
public class RepositoryException extends RuntimeException {

    /**
     * Creates an unchecked persistence error with its original cause.
     * @param message the detail message, which may be null
     * @param cause the underlying failure, which may be null
     */
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}