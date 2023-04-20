package de.tuberlin.mcc.simra.app.entities;

import android.content.Context;
import java.util.Map;
import de.tuberlin.mcc.simra.app.database.SimRaDB;


//TODO: Remove this whole class as it should not be needed anymore
// This might also apply to DataLog though the logic in that class
// is not as easily replaceable
/**
 * //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * // META-FILE (one per user): contains ...
 * // * the information required to display rides in the ride history (See RecorderService)
 * //   (DATE,START TIME, END TIME, ANNOTATED TRUE/FALSE)
 * // * the RIDE KEY which allows to identify the file containing the complete data for
 * //   a ride. => Use case: user wants to view a ride from history - retrieve data
 * // * one meta file per user, so we only want to create it if it doesn't exist yet.
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
    public static MetaDataEntry getMetadataEntryForRide(int rideId, Context context) {
        return SimRaDB.getDataBase(context).getMetaDataDao().getMetadataEntryForRide(rideId);
    }

    /**
     * Returns all MetadataEntries sorted by the rideId (lowest to highest)
     * @param context
     * @return Array of MetadataEntries sorted by the rideId
     */
    public static MetaDataEntry[] getMetadataEntriesSortedByKey(Context context) {
        return SimRaDB.getDataBase(context).getMetaDataDao().getMetadataEntriesSortedByKey();
    }

    /**
     * Updates a given MetadataEntry if it exists. If not a new entry is added to the db
     * @param entry the MetadataEntry
     * @param context
     */
    public static void updateOrAddMetadataEntryForRide(MetaDataEntry entry, Context context) {
        SimRaDB.getDataBase(context).getMetaDataDao().updateOrAddMetadataEntryForRide(entry);
    }

    public static void deleteMetadataEntryForRide(int rideId, Context context) {
        SimRaDB.getDataBase(context).getMetaDataDao().deleteMetadataEntryForRide(rideId);
    }

    //TODO: Check if this should really be here and not in MetaDataEntry
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
