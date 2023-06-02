package de.tuberlin.mcc.simra.app.database;

import android.content.Context;

import androidx.room.Dao;
import androidx.room.Transaction;

import java.util.List;

import de.tuberlin.mcc.simra.app.entities.DataLog;
import de.tuberlin.mcc.simra.app.entities.DataLogEntry;
import de.tuberlin.mcc.simra.app.entities.IncidentLog;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import de.tuberlin.mcc.simra.app.entities.MetaData;
import de.tuberlin.mcc.simra.app.entities.MetaDataEntry;
import kotlin.Pair;

/**
 * Dao implementation combining queries that occur right after each other into one transaction for
 * improved performance
 */
@Dao
public interface CombinedDao {

    @Transaction
    default void insertAll(List<DataLogEntry> dataLogEntries,  IncidentLog incidentLog, MetaDataEntry metaDataEntry, Context context) {
        DataLog.saveDataLogEntries(dataLogEntries, context);
        IncidentLog.saveIncidentLog(incidentLog, context);
        MetaData.updateOrAddMetadataEntryForRide(metaDataEntry, context);
    }

    @Transaction
    default Pair<DataLog, IncidentLogEntry[]> readDataAndIncidents(int rideId, Context context) {
        return new Pair<>(DataLog.loadDataLog(rideId, context),
                IncidentLog.loadIncidentLogEntriesOfRide(rideId, context));
    }

    @Transaction
    default void deleteAll(Integer rideId, Context context) {
        MetaData.deleteMetadataEntryForRide(rideId, context);
        DataLog.deleteEntriesOfRide(rideId, context);
        IncidentLog.deleteIncidentsOfRide(rideId, context);
    }
}
