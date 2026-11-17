package repository;

import domain.HasId;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Provides access to entities by identifier. Validation, null handling and
 * persistence depend on the implementation; see its operation documentation.
 * Implementations support MEMORY, FILE and PostgreSQL persistence.
 *
 * @param <ID> the identifier type
 * @param <E> the entity type exposing that identifier
 */
public interface Repository<ID, E extends HasId<ID>> {
    /**
     * Looks up an entity by identifier.
     * @param id the identifier; null handling is implementation-specific
     * @return the matching entity, or an empty optional when absent
     * @throws RepositoryException if persistence access fails
     */
    Optional<E> findOne(ID id);
    /**
     * Retrieves the entities currently available to this repository.
     * @return an iterable of entities, empty when none exist; ordering and
     *         mutability depend on the implementation
     * @throws RepositoryException if persistence access fails
     */
    Iterable<E> findAll();
    /**
     * Returns the entities that satisfy the given predicate.
     *
     * @param rule the predicate that determines whether to include an entity
     * @return a list of matching entities, or an empty list if none match
     * @throws NullPointerException if the predicate is null
     * @throws RepositoryException if persistence access fails
     */
    default List<E> findAll(Predicate<E> rule)
    {
        Iterable<E> entities = findAll();
        Spliterator<E> spliterator = entities.spliterator();
        var result = StreamSupport.stream(spliterator, false).filter(rule);
        return result.collect(Collectors.toList());
    }
    /**
     * Inserts an entity without replacing an existing entity with the same ID.
     * PostgreSQL user storage requires a null ID and assigns the generated ID
     * to the supplied entity. Other implementations require an explicit ID.
     * @param entity the entity to insert; acceptance rules depend on the implementation
     * @throws IllegalArgumentException if the entity is rejected by repository
     *         argument checks or its identifier is already stored
     * @throws validation.ValidationException if an installed validator rejects the entity
     * @throws RepositoryException if persistence fails
     */
    void save(E entity);
    /**
     * Removes the entity with the supplied identifier.
     * @param id the non-null identifier to remove
     * @return the deleted entity, or an empty optional if the ID is absent
     * @throws IllegalArgumentException if id is null
     * @throws RepositoryException if persistence fails
     */
    Optional<E> delete(ID id);
    /**
     * Replaces the stored entity with the same identifier.
     * Validates the replacement when a validator is installed, before changing
     * storage. Validation failure leaves the stored entity unchanged.
     * @param entity the non-null replacement with a non-null ID
     * @return an empty optional on success, or the supplied entity if its ID is absent
     * @throws validation.ValidationException if an installed validator rejects the entity
     * @throws IllegalArgumentException if the entity or its ID is null
     * @throws RepositoryException if persistence fails
     */
    Optional<E> update(E entity);
}
