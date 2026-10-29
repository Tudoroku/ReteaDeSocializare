package validation;

/**
 * Checks entities against the rules defined by a concrete validator.
 * @param <T> the entity type to validate
 */
public interface Validator<T> {
    /**
     * Checks an entity and returns normally when all applicable rules hold.
     * @param entity the entity to check; concrete validators define null handling
     * @throws ValidationException if the entity violates a validation rule
     */
    void validate(T entity);
}
