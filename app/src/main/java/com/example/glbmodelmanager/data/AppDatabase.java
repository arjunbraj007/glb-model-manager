package com.example.glbmodelmanager.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main database class using Room
 * Singleton pattern ensures only one database instance exists
 */
@Database(entities = {User.class, GlbModel.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // Abstract methods to get DAOs
    public abstract UserDao userDao();
    public abstract GlbModelDao glbModelDao();

    // Singleton instance
    private static volatile AppDatabase INSTANCE;

    // Thread pool for database operations
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    /**
     * Get database instance
     * Thread-safe using synchronized block
     */
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "glb_model_database"
                            )
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Callback to populate database with default users
     * Runs when database is created for the first time
     */
    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            // Insert default users in background thread
            databaseWriteExecutor.execute(() -> {
                UserDao userDao = INSTANCE.userDao();

                // Create default admin user
                User admin = new User("admin", "admin123", "Admin");
                userDao.insert(admin);

                // Create default regular user
                User user = new User("user", "user123", "User");
                userDao.insert(user);
            });
        }
    };
}
