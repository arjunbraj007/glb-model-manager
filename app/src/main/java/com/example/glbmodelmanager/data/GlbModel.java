package com.example.glbmodelmanager.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * GLB Model entity for storing 3D model information
 * Stores metadata about GLB files, actual file stored in internal storage
 */
@Entity(tableName = "glb_models")
public class GlbModel {

    @PrimaryKey(autoGenerate = true)
    private int id;              // Auto-generated unique ID

    private String name;         // Display name of the model
    private String fileName;     // Actual file name in storage
    private String filePath;     // Full path to file in internal storage
    private long fileSize;       // Size in bytes
    private long addedDate;      // Timestamp when added (milliseconds)

    // Constructor
    public GlbModel(String name, String fileName, String filePath, long fileSize, long addedDate) {
        this.name = name;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.addedDate = addedDate;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(long addedDate) {
        this.addedDate = addedDate;
    }
}