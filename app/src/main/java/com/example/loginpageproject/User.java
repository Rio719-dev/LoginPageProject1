package com.example.loginpageproject;

import java.util.ArrayList;
import java.util.List;

public class User {
    public String fullName;
    public String birthday;
    public String address;
    public String email;
    public String mobile;
    public String password;
    public String accessType;
    public List<String> passwordHistory = new ArrayList<>();

    public User(String fullName, String birthday, String address, String email, String mobile, String password, String accessType) {
        this.fullName = fullName;
        this.birthday = birthday;
        this.address = address;
        this.email = email;
        this.mobile = mobile;
        this.password = password;
        this.accessType = accessType;
        this.passwordHistory.add(password);
    }
}