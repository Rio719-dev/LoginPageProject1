package com.example.loginpageproject.domain.model;

import java.util.ArrayList;
import java.util.List;

public class User {
    private int id;
    private String fullName;
    private String birthday;
    private String address;
    private String email;
    private String mobile;
    private String password;
    private String accessType;
    private List<String> passwordHistory;

    public User() {
        this.passwordHistory = new ArrayList<>();
    }

    public User(String fullName, String birthday, String address, String email, String mobile, String password, String accessType) {
        this.fullName = fullName;
        this.birthday = birthday;
        this.address = address;
        this.email = email;
        this.mobile = mobile;
        this.password = password;
        this.accessType = accessType;
        this.passwordHistory = new ArrayList<>();
        this.passwordHistory.add(password);
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getAccessType() { return accessType; }
    public void setAccessType(String accessType) { this.accessType = accessType; }
    public List<String> getPasswordHistory() { return passwordHistory; }
    public void setPasswordHistory(List<String> passwordHistory) { this.passwordHistory = passwordHistory; }
}