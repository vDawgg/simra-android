package de.tuberlin.mcc.simra.app.util;

import android.content.Context;
import android.os.Build;
import android.os.Debug;


import java.util.ArrayList;
import java.util.List;

public class ResourceUsage {
    static long pollingIntervalMillis = 1;
    static List<Integer> memMeasurements = new ArrayList<>();
    static volatile boolean isPolling = true;

    static Thread pollingThread;

    // Method to start the polling
    public static void startPollingMem(Context context) {
        isPolling = true;
        memMeasurements.clear();

        Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memoryInfo);

        pollingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isPolling) {

                    memMeasurements.add(memoryInfo.getTotalPss());

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
    public static double getAveragePSS() {
        isPolling = false;
        // Wait for the polling thread to finish
        try {
            pollingThread.join();
            List<Integer> MemList = memMeasurements;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return MemList.stream().mapToDouble(d -> d).average().orElse(0.0);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public static long getCpuUtilization() {
        return Debug.threadCpuTimeNanos();
    }
}

