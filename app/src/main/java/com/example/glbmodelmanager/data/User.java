package com.example.glbmodelmanager.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * User entity for storing user login information
 * This represents a table in our Room database
 */
@Entity(tableName = "users")
public class User {

    @PrimaryKey(autoGenerate = true)
    private int id;           // Auto-generated unique ID

    private String username;  // Username for login
    private String password;  // Password (in real app, should be hashed)
    private String role;      // Either "Admin" or "User"

    // Constructor
    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
