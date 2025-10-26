package com.example.proyectopresionarterial;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface RecommendationCacheDao {
    @Query("SELECT * FROM recommendation_cache WHERE id = 1 LIMIT 1")
    RecommendationCache getNow();

    @Query("SELECT * FROM recommendation_cache WHERE id = 1 LIMIT 1")
    LiveData<RecommendationCache> observe();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(RecommendationCache cache);

    @Query("DELETE FROM recommendation_cache")
    void clear();
}

