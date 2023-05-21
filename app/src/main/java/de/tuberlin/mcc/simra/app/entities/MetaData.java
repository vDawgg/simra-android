package de.tuberlin.mcc.simra.app.entities;

import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.Map;
import de.tuberlin.mcc.simra.app.database.SimRaDB;


/**
 * //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * // METADATA: contains ...
 * // * the information required to display rides in the ride history (See RecorderService)
 * //   (DATE,START TIME, END TIME, ANNOTATED TRUE/FALSE)
 * // * the RIDE KEY which allows to identify the file containing the complete data for
 * //   a ride. => Use case: user wants to view a ride from history - retrieve data
 * // * the RIDE KEY which allows to identify the the actual ride-data containing the complete data
 * //   for a ride. => Use case: user wants to view a ride from history - retrieve data
 */
public class MetaData {
    public final static String METADATA_HEADER = "key,startTime,endTime,state,numberOfIncidents,waitedTime,distance,numberOfScary,region";
    private Map<Integer, MetaDataEntry> metaDataEntries;

    public MetaData(Map<Integer, MetaDataEntry> metaDataEntries) {
        this.metaDataEntries = metaDataEntries;
    }

    //TODO: Add this to the original measurements as well, as it was omitted there
    /**
     * Returns the MetadataEntry for a specified ride
     * @param rideId the given rideId
     * @param context
     * @return
     */
    public static MetaDataEntry getMetadataEntryForRide(int rideId, Context context) {
        long start = System.nanoTime();
        MetaDataEntry entry = SimRaDB.getDataBase(context).getMetaDataDao().getMetadataEntryForRide(rideId);
        Log.d("BENCHMARK", "Loading metadata-entry took: "+(System.nanoTime()-start));
        return entry;
    }

    /**
     * Returns all MetadataEntries sorted by the rideId (lowest to highest)
     * @param context
     * @return Array of MetadataEntries sorted by the rideId
     */
    public static MetaDataEntry[] getMetadataEntriesSortedByKey(Context context) {
        long start = System.nanoTime();
        MetaDataEntry[] entries = SimRaDB.getDataBase(context).getMetaDataDao().getMetadataEntriesSortedByKey();
        Log.d("BENCHMARK", "Loading matadata took: "+(System.nanoTime()-start));
        return entries;
    }

    /**
     * Returns all MetaDataEntries sorted by the date they were last modified in descending order
     * @param context
     * @return
     */
    public static MetaDataEntry[] getMetaDataEntriesLastModified(Context context) {
        return SimRaDB.getDataBase(context).getMetaDataDao().getMetadataEntriesLastModified();
    }

    /**
     * Updates a given MetadataEntry if it exists. If not a new entry is added to the db
     * @param entry the MetadataEntry
     * @param context
     */
    public static void updateOrAddMetadataEntryForRide(MetaDataEntry entry, Context context) {
        long start = System.nanoTime();
        SimRaDB.getDataBase(context).getMetaDataDao().updateOrAddMetadataEntryForRide(entry);
        Log.d("BENCHMARK", "Updating/adding metadata-entry took: "+(System.nanoTime()-start));
    }

    /**
     * Updates or adds multiple MetaDataEntries
     * @param entries
     * @param context
     */
    public static void updateOrAddMetadataEntries(List<MetaDataEntry> entries, Context context) {
        SimRaDB.getDataBase(context).getMetaDataDao().updateOrAddMetadataEntries(entries);
    }

    /**
     * Delete the MetaDataEntry for the given rideId
     * @param rideId the rideId for the MetaDataEntry to be deleted
     * @param context
     */
    public static void deleteMetadataEntryForRide(int rideId, Context context) {
        long start = System.nanoTime();
        SimRaDB.getDataBase(context).getMetaDataDao().deleteMetadataEntryForRide(rideId);
        Log.d("BENCHMARK", "Deleting matadata-entry took: "+(System.nanoTime()-start));
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
