package com.example.loginpageproject.domain.usecase;

import com.example.loginpageproject.domain.repository.UserRepository;

public class ResetPasswordUseCase {
    private final UserRepository repository;

    public ResetPasswordUseCase(UserRepository repository) {
        this.repository = repository;
    }

    public boolean execute(String email, String newPassword) {
        return repository.updatePassword(email, newPassword);
    }
}