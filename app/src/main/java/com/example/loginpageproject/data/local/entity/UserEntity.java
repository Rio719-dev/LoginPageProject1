package com.example.loginpageproject.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users", indices = {@Index(value = {"email"}, unique = true)})
public class UserEntity {
    @PrimaryKey(autoGenerate = true)
    public int userId;

    @ColumnInfo(name = "full_name")
    public String fullName;

    public String birthday;
    public String address;
    public String email;
    public String username;

    @ColumnInfo(name = "mobile_number")
    public String mobileNumber;

    @ColumnInfo(name = "password_hash")
    public String passwordHash;

    @ColumnInfo(name = "access_type")
    public String accessType;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    @ColumnInfo(name = "account_status")
    public String accountStatus;

    @ColumnInfo(name = "password_history")
    public String passwordHistory; // Comma separated hashes
}