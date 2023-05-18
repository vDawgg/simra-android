package de.tuberlin.mcc.simra.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Arrays;

import de.tuberlin.mcc.simra.app.entities.MetaData;
import de.tuberlin.mcc.simra.app.entities.MetaDataEntry;

/**
 * Log Helper Methods for easier debugging
 */
public class LogHelper {

    private static final String TAG = "LogHelper";

    public static void showKeyPrefs(Context context) {
        SharedPreferences keyPrefs = context.getApplicationContext()
                .getSharedPreferences("keyPrefs", Context.MODE_PRIVATE);
        Log.d(TAG, "===========================V=keyPrefs=V===========================");
        Object[] keyPrefsArray = keyPrefs.getAll().entrySet().toArray();
        for (Object keyPrefsEntry : keyPrefsArray) {
            Log.d(TAG, keyPrefsEntry + "");
        }
        Log.d(TAG, "===========================Λ=keyPrefs=Λ===========================");
    }

    public static void showMetadata(Context context) {
        Log.d(TAG, "===========================V=metaData=V===========================");
        for(MetaDataEntry me : MetaData.getMetadataEntriesSortedByKey(context)) {
            System.out.println(me.stringifyMetaDataEntry());
        }
        Log.d(TAG, "===========================Λ=metaData=Λ===========================");
    }

    public static void showStatistics(Context context) {
        SharedPreferences profilePrefs = context.getApplicationContext()
                .getSharedPreferences("Profile", Context.MODE_PRIVATE);
        Log.d(TAG, "===========================V=Statistics=V===========================");
        Log.d(TAG, "numberOfRides: " + profilePrefs.getInt("NumberOfRides", -1));
        Log.d(TAG, "Distance: " + profilePrefs.getLong("Distance", -1) + "m");
        Log.d(TAG, "Co2: " + profilePrefs.getLong("Co2", -1) + "g");
        Log.d(TAG, "Duration: " + profilePrefs.getLong("Duration", -1) + "ms");
        Log.d(TAG, "WaitedTime: " + profilePrefs.getLong("WaitedTime", -1) + "s");
        Log.d(TAG, "NumberOfIncidents: " + profilePrefs.getInt("NumberOfIncidents", -1));
        Log.d(TAG, "NumberOfScary: " + profilePrefs.getInt("NumberOfScary", -1));
        String[] buckets = new String[24];
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = i + ": " + profilePrefs.getFloat(String.valueOf(i), -1.0f);
        }
        Log.d(TAG, "timeBuckets: " + Arrays.toString(buckets));
        Log.d(TAG, "===========================Λ=Statistics=Λ===========================");
    }
}
