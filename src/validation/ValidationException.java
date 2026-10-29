package validation;

/** Reports an entity validation failure independently of repository argument errors. */
public class ValidationException extends RuntimeException{
    /**
     * Creates an unchecked validation error describing a violated rule.
     * @param message the detail message, which may be null
     */
    public ValidationException(String message)
    {
        super(message);
    }
}
