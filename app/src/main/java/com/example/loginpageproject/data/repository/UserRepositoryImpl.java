package com.example.loginpageproject.data.repository;

import com.example.loginpageproject.data.local.AuthDatabase;
import com.example.loginpageproject.domain.model.User;
import com.example.loginpageproject.domain.repository.UserRepository;

public class UserRepositoryImpl implements UserRepository {
    private final AuthDatabase database;

    public UserRepositoryImpl(AuthDatabase database) {
        this.database = database;
    }

    @Override
    public boolean registerUser(User user) {
        return database.insertUser(user);
    }

    @Override
    public User login(String email, String password) {
        User user = database.getUserByEmail(email);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    @Override
    public boolean checkEmailExists(String email) {
        return database.emailExists(email);
    }

    @Override
    public boolean updatePassword(String email, String newPassword) {
        User user = database.getUserByEmail(email);
        if (user == null) return false;
        
        user.getPasswordHistory().add(newPassword);
        StringBuilder history = new StringBuilder();
        for (String p : user.getPasswordHistory()) {
            history.append(p).append(",");
        }
        
        return database.updatePassword(email, newPassword, history.toString());
    }

    @Override
    public User getUserByEmail(String email) {
        return database.getUserByEmail(email);
    }
}