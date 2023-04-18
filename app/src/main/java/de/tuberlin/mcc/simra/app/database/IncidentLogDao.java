package de.tuberlin.mcc.simra.app.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import io.reactivex.Completable;

@Dao
public interface IncidentLogDao {

    //TODO: Find out if using a List here is really necessary
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void addOrUpdateIncidentLogEntries(List<IncidentLogEntry> entries);

    @Query("select * from incident_table where rideId == :rideId")
    public IncidentLogEntry[] loadIncidentLog(Integer rideId);

    @Query("delete from incident_table where id == :rideId")
    public void deleteEntriesOfRide(Integer rideId);

    //TODO: Find out if this is sensible
    @Query("delete from incident_table where id == :id")
    public void deleteIncidentLogEntry(Integer id);
}
