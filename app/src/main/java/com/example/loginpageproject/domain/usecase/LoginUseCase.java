package com.example.loginpageproject.domain.usecase;

import com.example.loginpageproject.domain.model.User;
import com.example.loginpageproject.domain.repository.UserRepository;

public class LoginUseCase {
    private final UserRepository repository;

    public LoginUseCase(UserRepository repository) {
        this.repository = repository;
    }

    public User execute(String email, String password) {
        return repository.login(email, password);
    }
}