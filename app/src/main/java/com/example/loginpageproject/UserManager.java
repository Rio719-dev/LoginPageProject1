package com.example.loginpageproject;

import java.util.HashMap;
import java.util.Map;

public class UserManager {
    private static UserManager instance;
    private Map<String, User> users = new HashMap<>();
    private User currentUser;

    private UserManager() {}

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public boolean registerUser(User user) {
        if (users.containsKey(user.email)) {
            return false;
        }
        users.put(user.email, user);
        return true;
    }

    public User login(String email, String password) {
        User user = users.get(email);
        if (user != null && user.password.equals(password)) {
            currentUser = user;
            return user;
        }
        return null;
    }

    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isEmailRegistered(String email) {
        return users.containsKey(email);
    }

    public boolean isPasswordReused(User user, String newPassword) {
        return user.passwordHistory.contains(newPassword);
    }

    public void updatePassword(User user, String newPassword) {
        user.password = newPassword;
        user.passwordHistory.add(newPassword);
    }
}