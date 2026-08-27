package com.example.loginpageproject.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.loginpageproject.data.local.entity.UserEntity;

import java.util.List;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertUser(UserEntity user);

    @Update
    void updateUser(UserEntity user);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    UserEntity getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE userId = :id LIMIT 1")
    UserEntity getUserById(int id);

    @Query("SELECT password_history FROM users WHERE email = :email")
    String getPasswordHistory(String email);

    @Query("UPDATE users SET password_hash = :newHash, password_history = :newHistory, updated_at = :updatedAt WHERE email = :email")
    void updatePassword(String email, String newHash, String newHistory, long updatedAt);
}