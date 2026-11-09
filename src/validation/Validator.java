package validation;

/**
 * Checks entities against the rules defined by a concrete validator.
 * @param <T> the entity type to validate
 */
@FunctionalInterface
public interface Validator<T> {
    /**
     * Checks an entity and returns normally when all applicable rules hold.
     * @param entity the entity to check; concrete validators define null handling
     * @throws ValidationException if the entity violates a validation rule
     */
    void validate(T entity);
    /**
     * Returns a validator that runs this validator first, followed by the
     * given validator. If the first validation throws an exception,
     * the second validation is skipped.
     *
     * @param other the validator to run after this one
     * @return a composed validator that applies both validations in order
     */
    default Validator<T> andThen(Validator<T> other) {
        return entity -> {
            this.validate(entity);
            other.validate(entity);
        };
    }
}
