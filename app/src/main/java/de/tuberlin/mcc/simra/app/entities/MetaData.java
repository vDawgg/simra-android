package de.tuberlin.mcc.simra.app.entities;

import android.content.Context;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import de.tuberlin.mcc.simra.app.database.SimRaDB;

/**
 * //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * // METADATA: contains ...
 * // * the information required to display rides in the ride history (See RecorderService)
 * //   (DATE,START TIME, END TIME, ANNOTATED TRUE/FALSE)
 * // * the RIDE KEY which allows to identify the the actual ride-data containing the complete data
 * //   for a ride. => Use case: user wants to view a ride from history - retrieve data
 */
public class MetaData {
    public final static String METADATA_HEADER = "key,startTime,endTime,state,numberOfIncidents,waitedTime,distance,numberOfScary,region";
    private Map<Integer, MetaDataEntry> metaDataEntries;

    public MetaData(Map<Integer, MetaDataEntry> metaDataEntries) {
        this.metaDataEntries = metaDataEntries;
    }

    /**
     * Returns the MetadataEntry for a specified ride
     * @param rideId the given rideId
     * @param context
     * @return
     */
    public static Future<MetaDataEntry> getMetadataEntryForRide(int rideId, Context context) {
        return SimRaDB.databaseWriteExecutor.submit(() ->
                SimRaDB.getDataBase(context).getMetaDataDao().getMetadataEntryForRide(rideId));
    }

    /**
     * Returns all MetadataEntries sorted by the rideId (lowest to highest)
     * @param context
     * @return Array of MetadataEntries sorted by the rideId
     */
    public static Future<MetaDataEntry[]> getMetadataEntriesSortedByKey(Context context) {
        return SimRaDB.databaseWriteExecutor.submit(() ->
                SimRaDB.getDataBase(context).getMetaDataDao().getMetadataEntriesSortedByKey());
    }

    /**
     * Returns all MetaDataEntries sorted by the date they were last modified in descending order
     * @param context
     * @return
     */
    public static Future<MetaDataEntry[]> getMetaDataEntriesLastModifies(Context context) {
        return SimRaDB.databaseWriteExecutor.submit(() ->
                SimRaDB.getDataBase(context).getMetaDataDao().getMetadataEntriesLastModified());
    }

    /**
     * Updates a given MetadataEntry if it exists. If not a new entry is added to the db
     * @param entry the MetadataEntry
     * @param context
     */
    public static Future<?> updateOrAddMetadataEntryForRide(MetaDataEntry entry, Context context) {
        return SimRaDB.databaseWriteExecutor.submit(() ->
                SimRaDB.getDataBase(context).getMetaDataDao().updateOrAddMetadataEntryForRide(entry));
    }

    /**
     * Updates or adds multiple MetaDataEntries
     * @param entries
     * @param context
     */
    public static Future<?> updateOrAddMetadataEntries(List<MetaDataEntry> entries, Context context) {
        return SimRaDB.databaseWriteExecutor.submit(() ->
                SimRaDB.getDataBase(context).getMetaDataDao().updateOrAddMetadataEntries(entries));
    }

    /**
     * Delete the MetaDataEntry for the given rideId
     * @param rideId the rideId for the MetaDataEntry to be deleted
     * @param context
     */
    public static Future<?> deleteMetadataEntryForRide(int rideId, Context context) {
        return SimRaDB.databaseWriteExecutor.submit(() ->
                SimRaDB.getDataBase(context).getMetaDataDao().deleteMetadataEntryForRide(rideId));
    }

    public static class STATE {
        /**
         * The ride is saved locally and was not yet annotated
         */
        public static final int JUST_RECORDED = 0;
        /**
         * The ride is saved locally and was annotated by the user
         */
        public static final int ANNOTATED = 1;
        /**
         * The ride is synced with the server and can not be edited anymore
         */
        public static final int SYNCED = 2;
    }
}
