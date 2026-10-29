package repository;

import domain.HasId;

import java.util.Optional;

/**
 * Provides access to entities by identifier. Validation, null handling and
 * persistence depend on the implementation; see its operation documentation.
 * Database placeholders may reject every operation as unsupported.
 *
 * @param <ID> the identifier type
 * @param <E> the entity type exposing that identifier
 */
public interface Repository<ID, E extends HasId<ID>> {
    /**
     * Looks up an entity by identifier.
     * @param id the identifier; null handling is implementation-specific
     * @return the matching entity, or an empty optional when absent
     * @throws UnsupportedOperationException if lookup is not implemented
     */
    Optional<E> findOne(ID id);
    /**
     * Retrieves the entities currently available to this repository.
     * @return an iterable of entities, empty when none exist; ordering and
     *         mutability depend on the implementation
     * @throws UnsupportedOperationException if retrieval is not implemented
     */
    Iterable<E> findAll();
    /**
     * Inserts an entity without replacing an existing entity with the same ID.
     * @param entity the entity to insert; acceptance rules depend on the implementation
     * @throws IllegalArgumentException if the entity is rejected by repository
     *         argument checks or its identifier is already stored
     * @throws validation.ValidationException if an installed validator rejects the entity
     * @throws RepositoryException if persistence fails
     * @throws UnsupportedOperationException if insertion is not implemented
     */
    void save(E entity);
    /**
     * Removes the entity with the supplied identifier.
     * @param id the identifier to remove; null handling is implementation-specific
     * @throws IllegalArgumentException if id is null or no entity has that identifier
     * @throws RepositoryException if persistence fails
     * @throws UnsupportedOperationException if deletion is not implemented
     */
    void delete(ID id);
    /**
     * Replaces the stored entity with the same identifier.
     * @param entity the replacement; acceptance rules depend on the implementation
     * @throws validation.ValidationException if an installed validator rejects the entity
     * @throws RepositoryException if persistence fails
     * @throws UnsupportedOperationException if replacement is not implemented
     */
    void update(E entity);
}
