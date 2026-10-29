package domain;

/**
 * Exposes an entity identifier without imposing validation or uniqueness rules.
 * @param <ID> the identifier type
 */
public interface HasId<ID> {
    /**
     * Returns the identifier supplied by the entity implementation.
     * @return the identifier; whether null is possible depends on the implementation
     */
    ID getId();
}
