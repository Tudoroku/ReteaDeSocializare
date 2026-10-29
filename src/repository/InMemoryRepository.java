package repository;

import domain.HasId;

import java.util.*;

/**
 * Stores entity references in a map without running a domain validator.
 * Only non-null entities with non-null, unique identifiers can be inserted.
 * The implicit public constructor creates an empty repository. Instances do not
 * persist data outside memory.
 * @param <ID> the identifier type
 * @param <E> the entity type
 */
public class InMemoryRepository<ID, E extends HasId<ID>> implements Repository<ID, E> {
    private final Map<ID, E> entities = new HashMap<>();

    /**
     * {@inheritDoc}
     * Returns the stored object itself. A null identifier yields an empty optional.
     */
    @Override
    public Optional<E> findOne(ID id) {
        if(entities.containsKey(id))
        {
            return Optional.of(entities.get(id));
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     * Returns an unmodifiable list snapshot containing the stored object
     * references. Later repository changes do not change the list; order is unspecified.
     */
    @Override
    public Iterable<E> findAll() {
        return List.copyOf(entities.values());
    }

    /**
     * {@inheritDoc}
     * Stores the supplied reference without validating its other fields.
     * Rejection leaves the existing mappings unchanged.
     * @throws IllegalArgumentException if the entity or its ID is null, or its ID exists
     */
    @Override
    public void save(E entity) {
        if(entity == null)
        {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        if(entity.getId() == null)
        {
            throw new IllegalArgumentException("Error: ID null");
        }
        if(entities.putIfAbsent(entity.getId(), entity) != null)
        {
            throw new IllegalArgumentException("ID " + entity.getId() + " already exists");
        }
    }

    /**
     * {@inheritDoc}
     * Removes only the identified mapping, without cascading to related entities.
     * @throws IllegalArgumentException if the ID is null or not stored
     */
    @Override
    public void delete(ID id) {
        if(entities.remove(id) == null)
        {
            throw new IllegalArgumentException("Invalid ID");
        }
    }

    /**
     * {@inheritDoc}
     * Stores the replacement reference without validating its other fields.
     * Rejection leaves existing mappings unchanged.
     * @throws IllegalArgumentException if the entity or its ID is null, or its ID is absent
     */
    @Override
    public void update(E entity) {
        if(entity == null)
        {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        if(entity.getId() == null)
        {
            throw new IllegalArgumentException("Error: ID null");
        }
        if(entities.replace(entity.getId(), entity) == null)
        {
            throw new IllegalArgumentException("Entity with id " + entity.getId() + " not found");
        }
    }
}
