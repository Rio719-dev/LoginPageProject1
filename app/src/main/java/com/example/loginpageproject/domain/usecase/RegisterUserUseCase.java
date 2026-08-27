package com.example.loginpageproject.domain.usecase;

import com.example.loginpageproject.domain.model.User;
import com.example.loginpageproject.domain.repository.UserRepository;

public class RegisterUserUseCase {
    private final UserRepository repository;

    public RegisterUserUseCase(UserRepository repository) {
        this.repository = repository;
    }

    public boolean execute(User user) {
        return repository.registerUser(user);
    }
}