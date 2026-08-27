package com.example.loginpageproject.domain.usecase;

import com.example.loginpageproject.domain.repository.UserRepository;

public class CheckEmailUseCase {
    private final UserRepository repository;

    public CheckEmailUseCase(UserRepository repository) {
        this.repository = repository;
    }

    public boolean execute(String email) {
        return repository.checkEmailExists(email);
    }
}