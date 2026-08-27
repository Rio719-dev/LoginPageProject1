package com.example.loginpageproject.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.loginpageproject.domain.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AuthDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "AuthSystem.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_USERS = "users";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_FULL_NAME = "full_name";
    public static final String COLUMN_BIRTHDAY = "birthday";
    public static final String COLUMN_ADDRESS = "address";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_MOBILE = "mobile";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_ACCESS_TYPE = "access_type";
    public static final String COLUMN_PASSWORD_HISTORY = "password_history";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_FULL_NAME + " TEXT, " +
                    COLUMN_BIRTHDAY + " TEXT, " +
                    COLUMN_ADDRESS + " TEXT, " +
                    COLUMN_EMAIL + " TEXT UNIQUE, " +
                    COLUMN_MOBILE + " TEXT, " +
                    COLUMN_PASSWORD + " TEXT, " +
                    COLUMN_ACCESS_TYPE + " TEXT, " +
                    COLUMN_PASSWORD_HISTORY + " TEXT" +
                    ");";

    public AuthDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    public boolean insertUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FULL_NAME, user.getFullName());
        values.put(COLUMN_BIRTHDAY, user.getBirthday());
        values.put(COLUMN_ADDRESS, user.getAddress());
        values.put(COLUMN_EMAIL, user.getEmail());
        values.put(COLUMN_MOBILE, user.getMobile());
        values.put(COLUMN_PASSWORD, user.getPassword());
        values.put(COLUMN_ACCESS_TYPE, user.getAccessType());
        
        StringBuilder history = new StringBuilder();
        for (String p : user.getPasswordHistory()) {
            history.append(p).append(",");
        }
        values.put(COLUMN_PASSWORD_HISTORY, history.toString());

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public User getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, COLUMN_EMAIL + "=?", new String[]{email}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            User user = new User();
            user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            user.setFullName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULL_NAME)));
            user.setBirthday(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BIRTHDAY)));
            user.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)));
            user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)));
            user.setMobile(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MOBILE)));
            user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)));
            user.setAccessType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACCESS_TYPE)));
            
            String historyStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_HISTORY));
            if (historyStr != null && !historyStr.isEmpty()) {
                user.getPasswordHistory().addAll(Arrays.asList(historyStr.split(",")));
            }
            cursor.close();
            return user;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    public boolean updatePassword(String email, String newPassword, String history) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PASSWORD, newPassword);
        values.put(COLUMN_PASSWORD_HISTORY, history);

        int result = db.update(TABLE_USERS, values, COLUMN_EMAIL + "=?", new String[]{email});
        return result > 0;
    }

    public boolean emailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_ID}, COLUMN_EMAIL + "=?", new String[]{email}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }
}