package com.example.proyectopresionarterial;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {BloodPressureRecord.class, UserProfile.class, RecommendationCache.class},
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract BloodPressureDao bloodPressureDao();
    public abstract UserProfileDao userProfileDao();
    public abstract RecommendationCacheDao recommendationCacheDao();

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `user_profile` (" +
                    "`id` INTEGER NOT NULL, " +
                    "`weightLbs` REAL, " +
                    "`heightCm` INTEGER, " +
                    "`dateOfBirth` TEXT, " +
                    "`gender` TEXT, " +
                    "`lastUpdated` TEXT, " +
                    "PRIMARY KEY(`id`))");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `recommendation_cache` (`id` INTEGER NOT NULL, `recommendation` TEXT, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "blood_pressure_db"
                            )
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
