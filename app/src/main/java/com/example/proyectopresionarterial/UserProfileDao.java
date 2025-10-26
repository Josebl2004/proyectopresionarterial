package com.example.proyectopresionarterial;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserProfile profile);

    @Query("SELECT * FROM user_profile LIMIT 1")
    LiveData<UserProfile> getProfile();

    // Síncrono para uso en workers/sync
    @Query("SELECT * FROM user_profile LIMIT 1")
    UserProfile getProfileNow();
}

