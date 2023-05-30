package de.tuberlin.mcc.simra.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import de.tuberlin.mcc.simra.app.entities.DataLogEntry;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import de.tuberlin.mcc.simra.app.entities.MetaDataEntry;
import kotlin.Pair;

@Dao
public interface CombinedDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertDataLogEntries(List<DataLogEntry> entries);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void addOrUpdateIncidentLogEntries(List<IncidentLogEntry> entries);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void updateOrAddMetadataEntryForRide(MetaDataEntry entry);

    @Query("delete from data_log_table where rideId == :rideId")
    public void deleteEntriesOfRide(Integer rideId);

    @Query("delete from metadata_table where rideId == :rideId")
    public void deleteMetadataEntryForRide(Integer rideId);

    @Query("delete from incident_table where rideId == :rideId")
    public void deleteIncidentLogEntries(Integer rideId);

    @Query("select * from incident_table where rideId == :rideId")
    public IncidentLogEntry[] loadIncidentLog(Integer rideId);

    @Query("SELECT * FROM data_log_table WHERE rideId == :rideId")
    public DataLogEntry[] loadAllEntriesOfRide(Integer rideId);

    @Transaction
    public default void insertAll(List<DataLogEntry> dataLogEntries, List<IncidentLogEntry> incidentLogEntries, MetaDataEntry metaDataEntries) {
        insertDataLogEntries(dataLogEntries);
        addOrUpdateIncidentLogEntries(incidentLogEntries);
        updateOrAddMetadataEntryForRide(metaDataEntries);
    }

    @Transaction
    public default Pair<DataLogEntry[], IncidentLogEntry[]> readAll(int rideId) {
        return new Pair<>(loadAllEntriesOfRide(rideId), loadIncidentLog(rideId));
    }

    @Transaction
    public default void deleteAll(Integer rideId) {
        deleteEntriesOfRide(rideId);
        deleteMetadataEntryForRide(rideId);
        deleteIncidentLogEntries(rideId);
    }
}
