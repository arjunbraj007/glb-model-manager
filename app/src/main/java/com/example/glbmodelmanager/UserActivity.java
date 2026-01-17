package com.example.glbmodelmanager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.glbmodelmanager.adapter.GlbModelAdapter;
import com.example.glbmodelmanager.data.AppDatabase;
import com.example.glbmodelmanager.data.GlbModel;
import com.example.glbmodelmanager.databinding.ActivityUserBinding;
import com.example.glbmodelmanager.utils.SessionManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * User Activity - Dashboard for regular users
 * Allows viewing GLB models only (no add/delete permissions)
 */
public class UserActivity extends AppCompatActivity implements GlbModelAdapter.OnItemClickListener {

    private ActivityUserBinding binding;
    private AppDatabase database;
    private SessionManager sessionManager;
    private GlbModelAdapter adapter;

    // Storage permission code
    private static final int STORAGE_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize database and session
        database = AppDatabase.getDatabase(this);
        sessionManager = new SessionManager(this);

        // Check storage permission
        checkStoragePermission();

        // Set up toolbar
        setSupportActionBar(binding.toolbar);

        // Display welcome message with username
        binding.tvWelcome.setText("Welcome, " + sessionManager.getUsername());

        // Set up RecyclerView
        setupRecyclerView();

        // Observe database changes
        observeModels();
    }

    /**
     * Check and request storage permission
     */
    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
            }
        }
    }

    /**
     * Set up RecyclerView with adapter
     */
    private void setupRecyclerView() {
        adapter = new GlbModelAdapter(false, this); // false = isAdmin (user mode)
        binding.rvModels.setLayoutManager(new LinearLayoutManager(this));
        binding.rvModels.setAdapter(adapter);
    }

    /**
     * Observe changes in GLB models from database
     */
    private void observeModels() {
        database.glbModelDao().getAllModels().observe(this, models -> {
            if (models == null || models.isEmpty()) {
                // Show empty state
                binding.rvModels.setVisibility(View.GONE);
                binding.tvEmptyState.setVisibility(View.VISIBLE);
            } else {
                // Show models list
                binding.rvModels.setVisibility(View.VISIBLE);
                binding.tvEmptyState.setVisibility(View.GONE);
                adapter.submitList(models);
            }
        });
    }

    /**
     * View button clicked (from adapter callback)
     */
    @Override
    public void onViewClick(GlbModel model) {
        viewModel(model);
    }

    /**
     * Delete button clicked (not used in User mode, but required by interface)
     */
    @Override
    public void onDeleteClick(GlbModel model) {
        // Not used for User role
    }

    /**
     * View GLB model - Show options dialog
     */
    private void viewModel(GlbModel model) {
        File file = new File(model.getFilePath());

        if (!file.exists()) {
            Toast.makeText(this, "Model file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show dialog with options
        new AlertDialog.Builder(this)
                .setTitle("Open 3D Model")
                .setMessage("How would you like to open: " + model.getName() + "?")
                .setPositiveButton("GLB Viewer", (dialog, which) ->
                        openWithGlbViewer(file, model.getName()))
                .setNegativeButton("Other Apps", (dialog, which) ->
                        openWithOtherApps(file))
                .setNeutralButton("Cancel", null)
                .show();
    }

    /**
     * Open with GLB Viewer app
     */
    private void openWithGlbViewer(File sourceFile, String modelName) {
        // Check if GLB Viewer is installed
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.mindblown.glbviewer");

        if (launchIntent == null) {
            // App not installed
            new AlertDialog.Builder(this)
                    .setTitle("GLB Viewer Not Installed")
                    .setMessage("Would you like to install GLB Viewer from Play Store?")
                    .setPositiveButton("Install", (dialog, which) -> {
                        try {
                            Intent playStoreIntent = new Intent(Intent.ACTION_VIEW);
                            playStoreIntent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.mindblown.glbviewer"));
                            startActivity(playStoreIntent);
                        } catch (Exception e) {
                            Toast.makeText(this, "Cannot open Play Store", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        // GLB Viewer is installed - copy file to Downloads folder
        try {
            // Get Downloads directory
            File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
            );

            // Create subdirectory for our app
            File glbDir = new File(downloadsDir, "GLBModels");
            if (!glbDir.exists()) {
                glbDir.mkdirs();
            }

            // Copy file to Downloads
            String fileName = modelName + ".glb";
            File destFile = new File(glbDir, fileName);

            // Copy file
            copyFile(sourceFile, destFile);

            // Show instructions dialog
            String instructions = "File saved to:\nDownloads/GLBModels/" + fileName +
                    "\n\n1. GLB Viewer will open now\n2. Tap the menu (☰)\n3. Select 'Open File'\n4. Navigate to Downloads/GLBModels\n5. Select: " + fileName;

            new AlertDialog.Builder(this)
                    .setTitle("Opening GLB Viewer")
                    .setMessage(instructions)
                    .setPositiveButton("Open GLB Viewer", (dialog, which) -> {
                        try {
                            startActivity(launchIntent);
                        } catch (Exception e) {
                            Toast.makeText(this, "Cannot open GLB Viewer", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .setCancelable(false)
                    .show();

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Copy file from source to destination
     */
    private void copyFile(File source, File dest) throws Exception {
        FileInputStream input = new FileInputStream(source);
        FileOutputStream output = new FileOutputStream(dest);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = input.read(buffer)) > 0) {
            output.write(buffer, 0, length);
        }

        output.flush();
        output.close();
        input.close();
    }

    /**
     * Open with other apps using system chooser
     */
    private void openWithOtherApps(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(intent, "Open with");
            startActivity(chooser);

        } catch (Exception e) {
            Toast.makeText(this,
                    "Error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Inflate menu in toolbar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user_menu, menu);
        return true;
    }

    /**
     * Handle menu item clicks
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Logout and return to login screen
     */
    private void logout() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}