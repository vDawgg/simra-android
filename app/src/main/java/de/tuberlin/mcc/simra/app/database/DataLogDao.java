package de.tuberlin.mcc.simra.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import de.tuberlin.mcc.simra.app.entities.DataLogEntry;

@Dao
public interface DataLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertDataLogEntries(List<DataLogEntry> entries);

    @Query("SELECT * FROM data_log_table WHERE rideId == :rideId")
    public DataLogEntry[] loadAllEntriesOfRide(Integer rideId);

    @Query("delete from data_log_table where rideId == :rideId")
    public void deleteEntriesOfRide(Integer rideId);

    @Query("delete from data_log_table where rideId == :rideId " +
            "and (timestamp < :startTime or timestamp > :endTime)")
    public void updateDataLogBoundaries(int rideId, long startTime, long endTime);
}
