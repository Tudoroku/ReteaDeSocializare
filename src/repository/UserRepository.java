package repository;

import domain.User;

/** Repository specialization for users identified by {@link Long} values. */
public interface UserRepository extends Repository<Long, User>{

}
