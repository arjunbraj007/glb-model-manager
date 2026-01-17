package com.example.glbmodelmanager;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.glbmodelmanager.adapter.GlbModelAdapter;
import com.example.glbmodelmanager.data.AppDatabase;
import com.example.glbmodelmanager.data.GlbModel;
import com.example.glbmodelmanager.databinding.ActivityAdminBinding;
import com.example.glbmodelmanager.utils.SessionManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Admin Activity - Dashboard for administrators
 * Allows adding, viewing, and deleting GLB models
 */
public class AdminActivity extends AppCompatActivity implements GlbModelAdapter.OnItemClickListener {

    private ActivityAdminBinding binding;
    private AppDatabase database;
    private SessionManager sessionManager;
    private GlbModelAdapter adapter;

    // Storage permission code
    private static final int STORAGE_PERMISSION_CODE = 100;

    /**
     * Activity Result Launcher for file picker
     * Handles the result when user selects a GLB file
     */
    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) {
                                handleSelectedFile(uri);
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBinding.inflate(getLayoutInflater());
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

        // Set up Add Model button
        binding.btnAddModel.setOnClickListener(v -> openFilePicker());

        // Observe database changes and update UI
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
     * Set up RecyclerView with adapter and layout manager
     */
    private void setupRecyclerView() {
        adapter = new GlbModelAdapter(true, this); // true = isAdmin
        binding.rvModels.setLayoutManager(new LinearLayoutManager(this));
        binding.rvModels.setAdapter(adapter);
    }

    /**
     * Observe changes in GLB models from database
     * Updates RecyclerView when data changes
     */
    private void observeModels() {
        database.glbModelDao().getAllModels().observe(this, models -> {
            if (models == null || models.isEmpty()) {
                // Show empty state message
                binding.rvModels.setVisibility(View.GONE);
                binding.tvEmptyState.setVisibility(View.VISIBLE);
            } else {
                // Show list of models
                binding.rvModels.setVisibility(View.VISIBLE);
                binding.tvEmptyState.setVisibility(View.GONE);
                adapter.submitList(models);
            }
        });
    }

    /**
     * Open file picker to select GLB file
     */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Accept all file types
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        String[] mimeTypes = {"model/gltf-binary", "application/octet-stream"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        filePickerLauncher.launch(Intent.createChooser(intent, "Select GLB File"));
    }

    /**
     * Handle selected GLB file
     * Copies file to internal storage and saves to database
     */
    private void handleSelectedFile(Uri uri) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Get file name from URI
                String fileName = getFileName(uri);

                // Validate file extension
                if (!fileName.toLowerCase().endsWith(".glb")) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Please select a .glb file", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                // Create unique file name to avoid conflicts
                long timestamp = System.currentTimeMillis();
                String uniqueFileName = timestamp + "_" + fileName;

                // Get internal storage directory
                File internalDir = new File(getFilesDir(), "glb_models");
                if (!internalDir.exists()) {
                    internalDir.mkdirs(); // Create directory if doesn't exist
                }

                // Create file in internal storage
                File destFile = new File(internalDir, uniqueFileName);

                // Copy file from URI to internal storage
                InputStream inputStream = getContentResolver().openInputStream(uri);
                FileOutputStream outputStream = new FileOutputStream(destFile);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                outputStream.close();
                inputStream.close();

                // Create database entry
                String displayName = fileName.replace(".glb", "").replace(".GLB", "");
                GlbModel model = new GlbModel(
                        displayName,
                        uniqueFileName,
                        destFile.getAbsolutePath(),
                        destFile.length(),
                        timestamp
                );

                // Save to database
                database.glbModelDao().insert(model);

                // Show success message on UI thread
                runOnUiThread(() ->
                        Toast.makeText(this, "Model added successfully!", Toast.LENGTH_SHORT).show()
                );

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error adding model: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    /**
     * Get file name from URI
     */
    private String getFileName(Uri uri) {
        String fileName = "model.glb";
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        return fileName;
    }

    /**
     * View GLB model using external app (from adapter callback)
     */
    @Override
    public void onViewClick(GlbModel model) {
        viewModel(model);
    }

    /**
     * Delete GLB model (from adapter callback)
     */
    @Override
    public void onDeleteClick(GlbModel model) {
        confirmDelete(model);
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
     * Show confirmation dialog before deleting
     */
    private void confirmDelete(GlbModel model) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Model")
                .setMessage("Are you sure you want to delete '" + model.getName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> deleteModel(model))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Delete GLB model from storage and database
     */
    private void deleteModel(GlbModel model) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Delete file from storage
                File file = new File(model.getFilePath());
                if (file.exists()) {
                    file.delete();
                }

                // Delete from database
                database.glbModelDao().delete(model);

                runOnUiThread(() ->
                        Toast.makeText(this, "Model deleted successfully", Toast.LENGTH_SHORT).show()
                );

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error deleting model: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    /**
     * Inflate menu in toolbar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_menu, menu);
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
