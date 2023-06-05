package de.tuberlin.mcc.simra.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;

@Dao
public interface IncidentLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void addOrUpdateIncidentLogEntries(List<IncidentLogEntry> entries);

    @Query("select * from incident_table where rideId == :rideId")
    public IncidentLogEntry[] loadIncidentLog(Integer rideId);

    @Query("delete from incident_table where rideId == :rideId")
    public void deleteIncidentLogEntries(Integer rideId);
}
