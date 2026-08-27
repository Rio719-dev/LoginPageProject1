package com.example.loginpageproject.domain.repository;

import com.example.loginpageproject.domain.model.User;
import java.util.List;

public interface UserRepository {
    boolean registerUser(User user);
    User login(String email, String password);
    boolean checkEmailExists(String email);
    boolean updatePassword(String email, String newPassword);
    User getUserByEmail(String email);
}