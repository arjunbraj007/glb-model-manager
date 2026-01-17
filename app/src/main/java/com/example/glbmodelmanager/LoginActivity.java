package com.example.glbmodelmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.glbmodelmanager.data.AppDatabase;
import com.example.glbmodelmanager.data.User;
import com.example.glbmodelmanager.databinding.ActivityLoginBinding;
import com.example.glbmodelmanager.utils.SessionManager;

/**
 * Login Activity - First screen of the app
 * Handles user authentication and redirects to appropriate dashboard
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AppDatabase database;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ViewBinding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize database and session manager
        database = AppDatabase.getDatabase(this);
        sessionManager = new SessionManager(this);

        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            redirectToDashboard(sessionManager.getUserRole());
            return;
        }

        // Set up login button click listener
        binding.btnLogin.setOnClickListener(v -> handleLogin());
    }

    /**
     * Handle login button click
     * Validates input and checks credentials
     */
    private void handleLogin() {
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // Validate input fields
        if (username.isEmpty()) {
            binding.etUsername.setError("Please enter username");
            binding.etUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            binding.etPassword.setError("Please enter password");
            binding.etPassword.requestFocus();
            return;
        }

        // Disable login button to prevent multiple clicks
        binding.btnLogin.setEnabled(false);

        // Perform login in background thread
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Check credentials in database
                User user = database.userDao().login(username, password);

                // Update UI on main thread
                runOnUiThread(() -> {
                    if (user != null) {
                        // Login successful - save session
                        sessionManager.saveSession(user.getId(), user.getUsername(), user.getRole());

                        // Show success message
                        Toast.makeText(LoginActivity.this,
                                "Welcome, " + user.getUsername() + "!",
                                Toast.LENGTH_SHORT).show();

                        // Redirect to appropriate dashboard
                        redirectToDashboard(user.getRole());
                    } else {
                        // Login failed - invalid credentials
                        Toast.makeText(LoginActivity.this,
                                "Invalid username or password",
                                Toast.LENGTH_SHORT).show();
                        binding.btnLogin.setEnabled(true);
                    }
                });
            } catch (Exception e) {
                // Handle any errors
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this,
                            "Login error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    binding.btnLogin.setEnabled(true);
                });
            }
        });
    }

    /**
     * Redirect user to appropriate dashboard based on role
     */
    private void redirectToDashboard(String role) {
        Intent intent;

        if ("Admin".equals(role)) {
            intent = new Intent(this, AdminActivity.class);
        } else if ("User".equals(role)) {
            intent = new Intent(this, UserActivity.class);
        } else {
            Toast.makeText(this, "Invalid user role", Toast.LENGTH_SHORT).show();
            return;
        }

        // Start dashboard activity and finish login activity
        startActivity(intent);
        finish(); // Prevent going back to login screen
    }
}

