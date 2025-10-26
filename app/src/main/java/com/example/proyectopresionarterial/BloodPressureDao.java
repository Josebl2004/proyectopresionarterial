package com.example.proyectopresionarterial;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BloodPressureDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(BloodPressureRecord record);

    @Query("SELECT * FROM blood_pressure_records WHERE id = :id LIMIT 1")
    BloodPressureRecord getById(long id);

    @Query("SELECT * FROM blood_pressure_records ORDER BY date DESC, time DESC")
    List<BloodPressureRecord> getAll();

    @Query("SELECT * FROM blood_pressure_records WHERE date = :date ORDER BY time DESC")
    List<BloodPressureRecord> getByDate(String date);

    @Query("SELECT * FROM blood_pressure_records WHERE synced = 0 ORDER BY date DESC, time DESC")
    List<BloodPressureRecord> getUnsynced();

    @Query("SELECT * FROM blood_pressure_records ORDER BY date DESC, time DESC")
    LiveData<List<BloodPressureRecord>> observeAllOrdered();

    @Query("SELECT * FROM blood_pressure_records ORDER BY date DESC, time DESC LIMIT 1")
    LiveData<BloodPressureRecord> observeLatest();

    @Query("SELECT * FROM blood_pressure_records WHERE date = :date ORDER BY time DESC LIMIT 1")
    LiveData<BloodPressureRecord> observeLatestForDate(String date);

    // Nuevo: registros entre fechas inclusive, orden ascendente para ejes del gráfico
    @Query("SELECT * FROM blood_pressure_records WHERE date BETWEEN :start AND :end ORDER BY date ASC, time ASC")
    LiveData<List<BloodPressureRecord>> observeBetweenDates(String start, String end);

    // Versión síncrona para StatsViewModel
    @Query("SELECT * FROM blood_pressure_records WHERE date BETWEEN :start AND :end ORDER BY date ASC, time ASC")
    List<BloodPressureRecord> getBetweenDates(String start, String end);

    @Update
    int update(BloodPressureRecord record);

    @Delete
    void delete(BloodPressureRecord record);

    @Query("SELECT * FROM blood_pressure_records ORDER BY date DESC, time DESC LIMIT 3")
    List<BloodPressureRecord> getLast3();
}
