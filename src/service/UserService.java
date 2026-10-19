package service;

import domain.User;
import repository.Repository;
import repository.UserRepository;
import validation.UserValidator;
import validation.Validator;


public class UserService {
    private final UserRepository repo;
    private final UserValidator validator;

    public UserService(UserRepository userRepo, UserValidator validator) {
        this.repo = userRepo;
        this.validator = validator;
    }
}
