package com.example.glbmodelmanager.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for GLB Models table
 * Defines methods to interact with 3D model data
 */
@Dao
public interface GlbModelDao {

    /**
     * Insert a new GLB model record
     */
    @Insert
    void insert(GlbModel model);

    /**
     * Delete a GLB model record
     */
    @Delete
    void delete(GlbModel model);

    /**
     * Get all GLB models, sorted by date (newest first)
     * LiveData automatically updates UI when data changes
     */
    @Query("SELECT * FROM glb_models ORDER BY addedDate DESC")
    LiveData<List<GlbModel>> getAllModels();

    /**
     * Get a specific model by ID
     */
    @Query("SELECT * FROM glb_models WHERE id = :id")
    GlbModel getModelById(int id);
}
