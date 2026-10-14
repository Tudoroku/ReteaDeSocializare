package repository;

import domain.HasId;

import java.util.Optional;

public interface Repository<ID, E extends HasId<ID>> {
    Optional<E> findOne(ID id);
    Iterable<E> findAll();
    void save(E entity);
    void delete(ID id);
    void update(E entity);
}
