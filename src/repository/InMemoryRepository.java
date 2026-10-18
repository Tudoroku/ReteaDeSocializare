package repository;

import domain.HasId;

import java.util.*;

public class InMemoryRepository<ID, E extends HasId<ID>> implements Repository<ID, E> {
    private final Map<ID, E> entities = new HashMap<>();

    @Override
    public Optional<E> findOne(ID id) {
        if(entities.containsKey(id))
        {
            return Optional.of(entities.get(id));
        }
        return Optional.empty();
    }

    @Override
    public Iterable<E> findAll() {
        return List.copyOf(entities.values());
    }

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

    @Override
    public void delete(ID id) {
        if(entities.remove(id) == null)
        {
            throw new IllegalArgumentException("Invalid ID");
        }
    }

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
