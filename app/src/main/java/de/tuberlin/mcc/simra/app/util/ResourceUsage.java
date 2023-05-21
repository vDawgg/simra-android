package de.tuberlin.mcc.simra.app.util;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Debug;
import android.os.Process;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class ResourceUsage {
    static long pollingIntervalMillis = 1;
    static List<Integer> currentMeasurements = new ArrayList<>();
    static volatile boolean isPolling = true;

    static Thread pollingThread;

    // Method to start the polling
    public static void startPollingCurrent(Context context) {
        isPolling = true;
        currentMeasurements.clear();
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);

        pollingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isPolling) {
                    int currentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                    currentMeasurements.add(currentNow);

                    try {
                        Thread.sleep(pollingIntervalMillis);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        pollingThread.start();
    }

    // Method to stop the polling
    public static List<Integer> getCurrent() {
        isPolling = false;
        // Wait for the polling thread to finish
        try {
            pollingThread.join();
            List<Integer> BatteryList = currentMeasurements;
            return BatteryList;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    //TODO: This should probably poll every few ms to get an average
    public static Pair<Integer, Integer> getUsedMemorySize() {
        Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memoryInfo);
        memoryInfo.getMemoryStats();
        return new Pair<>(memoryInfo.getTotalPss(), memoryInfo.getTotalPrivateDirty());
    }

    public static long getCpuUtilization() {
        return Debug.threadCpuTimeNanos();
    }
}
