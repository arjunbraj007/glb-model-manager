package com.example.glbmodelmanager.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

/**
 * Data Access Object for User table
 * Defines methods to interact with user data
 */
@Dao
public interface UserDao {

    /**
     * Insert a new user into database
     */
    @Insert
    void insert(User user);

    /**
     * Check if user exists with given username and password
     * Returns the user if found, null otherwise
     */
    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    User login(String username, String password);

    /**
     * Check if any users exist in database
     * Used to determine if we need to create default users
     */
    @Query("SELECT COUNT(*) FROM users")
    int getUserCount();
}
