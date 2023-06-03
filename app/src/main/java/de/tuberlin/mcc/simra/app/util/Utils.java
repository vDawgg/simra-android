package de.tuberlin.mcc.simra.app.util;

import static de.tuberlin.mcc.simra.app.activities.ProfileActivity.startProfileActivityForChooseRegion;
import static de.tuberlin.mcc.simra.app.util.IOUtils.zipDb;
import static de.tuberlin.mcc.simra.app.util.SimRAuthenticator.getClientHash;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.location.LocationManager;
import android.util.Log;
import android.util.Pair;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import javax.net.ssl.HttpsURLConnection;

import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.entities.DataLog;
import de.tuberlin.mcc.simra.app.entities.DataLogEntry;
import de.tuberlin.mcc.simra.app.entities.IncidentLog;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import de.tuberlin.mcc.simra.app.entities.MetaDataEntry;
import de.tuberlin.mcc.simra.app.entities.Profile;

public class Utils {

    private static final String TAG = "Utils_LOG";

    /**
     * @return content from file with given fileName as a String
     */
    public static String readContentFromFile(String fileName, Context context) {
        File file = new File(IOUtils.Directories.getBaseFolderPath(context) + fileName);
        if (file.isDirectory()) {
            return "FILE IS DIRECTORY";
        }
        StringBuilder content = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;

            while ((line = br.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
        } catch (IOException ioe) {
            Log.d(TAG, "readContentFromFile() Exception: " + Arrays.toString(ioe.getStackTrace()));
        }
        return content.toString();
    }

    /**
     * Produces the consolidated File which should be uploaded to the backend
     * concatenates content from IncidentLog and DataLog
     * removes incidents that are auto generated
     *
     * @param rideId
     * @param context
     * @return The ride to upload and the filtered incident log to overwrite after successful upload
     */
    public static Pair<String, IncidentLog> getConsolidatedRideForUpload(int rideId, Context context) {
        StringBuilder content = new StringBuilder();

        IncidentLogEntry[] incidentLogEntries = IncidentLog.loadIncidentLogEntriesOfRide(rideId, context);
        content.append(IOUtils.Files.getFileInfoLine(incidentLogEntries[0].nn_version))
                .append(IncidentLog.INCIDENT_LOG_HEADER)
                .append(System.lineSeparator());
        for (IncidentLogEntry incident : incidentLogEntries) {
            content.append(incident.stringifyDataLogEntry()).append(System.lineSeparator());
        }

        IncidentLog incidentLog = IncidentLog.filterIncidentLogUploadReady(IncidentLog.loadIncidentLog(rideId, context), null, null, null, null, true);
        String dataLog = DataLog.loadDataLog(rideId, context).toString();

        content.append(System.lineSeparator())
                .append("=========================")
                .append(System.lineSeparator())
                .append(dataLog);

        return new Pair<>(content.toString(), incidentLog);
    }

    public static void overwriteFile(String content, File file) {
        try {
            FileOutputStream writer = new FileOutputStream(file);
            writer.write(content.getBytes());
            writer.flush();
            writer.close();
        } catch (IOException ioe) {
            Log.d(TAG, Arrays.toString(ioe.getStackTrace()));
        }
    }

    public static void deleteErrorLogsForVersion(Context context, int version) {
        int appVersion = SharedPref.lookUpIntSharedPrefs("App-Version", -1, "simraPrefs", context);
        if (appVersion < version) {
            SharedPref.App.Crash.NewCrash.setEnabled(false, context);
            File[] dirFiles = context.getFilesDir().listFiles();
            String path;
            for (File dirFile : dirFiles) {
                path = dirFile.getName();
                if (path.startsWith("CRASH")) {
                    dirFile.delete();
                }
            }
        }
    }

    public static String[] getRegions(Context context) {
        String[] simRa_regions_config = (Utils.readContentFromFile("simRa_regions.config", context)).split(System.lineSeparator());

        return simRa_regions_config;
    }

    /**
     *
     * @param context
     * @return String[], where each element is one news element.
     */
    public static String[] getNews(Context context) {
        String locale = Resources.getSystem().getConfiguration().locale.getLanguage();
        boolean languageIsEnglish = locale.equals(new Locale("en").getLanguage());
        if (languageIsEnglish) {
            return readContentFromFile(IOUtils.Files.getENNewsFile(context).getName(),context).split(System.lineSeparator());
        } else {
            return readContentFromFile(IOUtils.Files.getDENewsFile(context).getName(),context).split(System.lineSeparator());
        }
    }

    public static boolean isInTimeFrame(Long startTimeBoundary, Long endTimeBoundary, long timestamp) {
        return (startTimeBoundary == null && endTimeBoundary == null) || (endTimeBoundary == null && timestamp >= startTimeBoundary) || (startTimeBoundary == null && timestamp <= endTimeBoundary) || (timestamp >= startTimeBoundary && timestamp <= endTimeBoundary);
    }

    // co2 savings on a bike: 138g/km
    public static long calculateCO2Savings(Long totalDistance) {
        return (long) ((totalDistance / (float) 1000) * 138);
    }

    // returns the incidents to be proposed and the neuronal network version that calculated the incidents (-1 if local algorithm was used)
    public static Pair<List<IncidentLogEntry>, Integer> findAccEvents(int rideId, int bike, int pLoc, int state, Context context) {
        List<IncidentLogEntry> foundEvents = null;
        Integer nn_version = 0;
        if (SharedPref.Settings.IncidentGenerationAIActive.getAIEnabled(context)) {
            Pair<List<IncidentLogEntry>, Integer> findAccEventOnlineResult = findAccEventOnline(rideId, bike, pLoc, context);
            foundEvents = findAccEventOnlineResult.first;
            nn_version = findAccEventOnlineResult.second;
        }
        if (foundEvents != null && foundEvents.size() > 0) {
            return new Pair<>(foundEvents, nn_version);
        }
        else {
            return findAccEventsLocal(rideId, state, context);
        }
    }

     // Uses sophisticated AI to analyze the ride
    public static Pair<List<IncidentLogEntry>, Integer> findAccEventOnline(int rideId, int bike, int pLoc, Context context) {
        try {
            StringBuilder responseBuilder = new StringBuilder();

            URL url = new URL(BuildConfig.API_ENDPOINT + BuildConfig.API_VERSION + "classify-ride-cyclesense?clientHash=" + getClientHash(context)
                    + "&os=android");

            Log.d(TAG, "URL for AI-Backend: " + url.toString());
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "text/plain");
            urlConnection.setRequestProperty("Accept","*/*");
            urlConnection.setDoOutput(true);
            urlConnection.setReadTimeout(30000);
            urlConnection.setConnectTimeout(30000);

            String dataLog = DataLog.loadDataLog(rideId, context).toString();
            byte[] outputInBytes = dataLog.getBytes(StandardCharsets.UTF_8);

            //upload byteArr
            Log.d(TAG, "send data: ");
            try (OutputStream os = urlConnection.getOutputStream()) {
                long startTime = System.currentTimeMillis();
                long uploadTimeoutMS = 30000;
                int chunkSize = 1024;
                int chunkIndex = 0;

                while (chunkSize * chunkIndex < outputInBytes.length) {
                    int offset = chunkSize * chunkIndex;
                    int remaining = outputInBytes.length - offset;
                    os.write(outputInBytes, offset, Math.min(remaining, chunkSize));
                    chunkIndex += 1;

                    //upload timeout
                    if(startTime + uploadTimeoutMS < System.currentTimeMillis())
                        return new Pair<>(null, -2);
                }

                os.flush();
                os.close();
            }

            // receive results
            Log.d(TAG, "receive data: ");
            int status = urlConnection.getResponseCode();
            Log.d(TAG, "Server status: " + status);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(urlConnection.getInputStream()
                    ));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                responseBuilder.append(inputLine);
            }
            in.close();

            String responseString = responseBuilder.toString();
            Log.d(TAG, "Server Message: " + responseString);

            // response okay
            if (status == 200 && responseString.length() > 1) {
                JSONArray incidentTimestamps = new JSONArray(responseString);
                Integer nn_version = (Integer) incidentTimestamps.remove(0); // remove first element since it is the nn_version
                List<IncidentLogEntry> foundIncidents = new ArrayList<>();
                DataLog allLogs = DataLog.loadDataLog(rideId, context);

                // for each gps data log entries loop through the incident timestamps and create an incident at position, if the timestamps match
                int key = 0;
                int index = 0;
                while (!allLogs.onlyGPSDataLogEntries.isEmpty() && incidentTimestamps.length() > 0 && index < allLogs.onlyGPSDataLogEntries.size()) {
                    DataLogEntry gpsLine = allLogs.onlyGPSDataLogEntries.get(index);
                    for (int i = 0; i < incidentTimestamps.length(); i++) {
                        if(gpsLine.timestamp == incidentTimestamps.getLong(i)) {
                            foundIncidents.add(IncidentLogEntry.newBuilder()
                                    .withBaseInformation(gpsLine.timestamp, gpsLine.latitude, gpsLine.longitude)
                                    .withIncidentType(IncidentLogEntry.INCIDENT_TYPE.AUTO_GENERATED)
                                    .withKey(key++)
                                    .withRideId(rideId)
                                    .build());
                            incidentTimestamps.remove(index);
                        }
                    }
                    index++;
                }

                return new Pair<>(foundIncidents, nn_version);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return new Pair<>(null, -2);
    }

    //TODO: Test why this returns fewer incidents than the old version
    public static Pair<List<IncidentLogEntry>, Integer> findAccEventsLocal(int rideId, int state, Context context) {
        class Event {
            final double lat;
            final double lon;
            final double maxXDelta;
            final double maxYDelta;
            final double maxZDelta;
            final long timeStamp;

            public Event(double lat, double lon, double maxXDelta, double maxYDelta, double maxZDelta, long timeStamp) {
                this.lat = lat;
                this.lon = lon;
                this.maxXDelta = maxXDelta;
                this.maxYDelta = maxYDelta;
                this.maxZDelta = maxZDelta;
                this.timeStamp = timeStamp;
            }
        }

        List<IncidentLogEntry> accEvents = new ArrayList<>(6);

        List<Event> events = new ArrayList<>(6);
        IncidentLogEntry.InvolvedRoadUser involvedRoadUser = new IncidentLogEntry.InvolvedRoadUser(false, false, false, false, false, false, false, false, false, false);
        accEvents.add(new IncidentLogEntry(0, 999.0, 999.0, 0L, 0, false, false, 0, 0, involvedRoadUser, false, ""));
        accEvents.add(new IncidentLogEntry(1, 999.0, 999.0, 0L, 0, false, false, 0, 0, involvedRoadUser, false, ""));
        accEvents.add(new IncidentLogEntry(2, 999.0, 999.0, 0L, 0, false, false, 0, 0, involvedRoadUser, false, ""));
        accEvents.add(new IncidentLogEntry(3, 999.0, 999.0, 0L, 0, false, false, 0, 0, involvedRoadUser, false, ""));
        accEvents.add(new IncidentLogEntry(4, 999.0, 999.0, 0L, 0, false, false, 0, 0, involvedRoadUser, false, ""));
        accEvents.add(new IncidentLogEntry(5, 999.0, 999.0, 0L, 0, false, false, 0, 0, involvedRoadUser, false, ""));

        Event template = new Event(0, 0, 0, 0, 0, 0);
        events.add(template);
        events.add(template);
        events.add(template);
        events.add(template);
        events.add(template);
        events.add(template);

        DataLogEntry[] entries = DataLog.loadDataLogEntriesOfRide(rideId, context);

        boolean newSubPart = false;

        for (int i = 0; i < entries.length; i++) {
            if (newSubPart) break;

            DataLogEntry entry = entries[i];

            double maxX = entry.accelerometerX;
            double minX = entry.accelerometerX;
            double maxY = entry.accelerometerY;
            double minY = entry.accelerometerY;
            double maxZ = entry.accelerometerZ;
            double minZ = entry.accelerometerZ;
            long timestamp = entry.timestamp;

            if (entries.length > i + 1 && entries[i + 1].latitude == null) {
                newSubPart = true;

                for (int j = i + 1; j < entries.length; j++) {
                    DataLogEntry tempEntry = entries[j];

                    maxX = (tempEntry.accelerometerX >= maxX) ? tempEntry.accelerometerX : maxX;
                    minX = (tempEntry.accelerometerX < minX) ? tempEntry.accelerometerX : minX;
                    maxY = (tempEntry.accelerometerY >= maxY) ? tempEntry.accelerometerY : maxY;
                    minY = (tempEntry.accelerometerY < minY) ? tempEntry.accelerometerY : minY;
                    maxZ = (tempEntry.accelerometerZ >= maxZ) ? tempEntry.accelerometerZ : maxZ;
                    minZ = (tempEntry.accelerometerZ < minZ) ? tempEntry.accelerometerZ : minZ;

                    if (entries.length > j + 1 && !(entries[j + 1].latitude == null)) {
                        entry = entries[j + 1];
                        newSubPart = false;
                        break;
                    }
                }
            }

            double maxXDelta = Math.abs(maxX - minX);
            double maxYDelta = Math.abs(maxY - minY);
            double maxZDelta = Math.abs(maxZ - minZ);

            double lat = (entry.latitude == null) ? 0f : entry.latitude;
            double lon = (entry.longitude == null) ? 0f : entry.longitude;
            Event currEvent = new Event(lat, lon, maxXDelta, maxYDelta, maxZDelta, timestamp);

            // Checks whether there is a minimum of <threshold> milliseconds
            // between the actual event and the top 6 events so far.
            int threshold = 10000; // 10 seconds
            long minTimeDelta = 999999999;
            for (Event event : events) {
                long actualTimeDelta = timestamp - event.timeStamp;
                if (actualTimeDelta < minTimeDelta) {
                    minTimeDelta = actualTimeDelta;
                }
            }
            boolean enoughTimePassed = minTimeDelta > threshold;

            // Check whether actualX is one of the top 2 events
            if (maxXDelta > events.get(0).maxXDelta && enoughTimePassed) {
                Event temp = events.get(0);
                events.set(0, currEvent);
                accEvents.set(0, new IncidentLogEntry(0, currEvent.lat, currEvent.lon, currEvent.timeStamp, 0, false, false, 0, 0, involvedRoadUser, false, ""));

                events.set(1, temp);
                accEvents.set(1, new IncidentLogEntry(1, temp.lat, temp.lon, temp.timeStamp, 0, false, false, 0, 0, involvedRoadUser, false, ""));
            } else if (maxXDelta > events.get(1).maxXDelta && enoughTimePassed) {
                events.set(1, currEvent);
                accEvents.set(1, new IncidentLogEntry(1, currEvent.lat, currEvent.lon, currEvent.timeStamp, 0, false, false, 0, 0, involvedRoadUser, false, ""));
            }
            // Check whether actualY is one of the top 2 events
            else if (maxYDelta > events.get(2).maxYDelta && enoughTimePassed) {
                Event temp = events.get(2);
                events.set(2, currEvent);
                accEvents.set(2, new IncidentLogEntry(2, currEvent.lat, currEvent.lon, currEvent.timeStamp, 0, false, false, 0, 0, involvedRoadUser, false, ""));
                events.set(3, temp);
                accEvents.set(3, new IncidentLogEntry(3, temp.lat, temp.lon, temp.timeStamp, 0, false, false, 0, 0, involvedRoadUser, false, ""));
            } else if (maxYDelta > events.get(3).maxYDelta && enoughTimePassed) {
                events.set(3, currEvent);
                accEvents.set(3, new IncidentLogEntry(3, currEvent.lat, currEvent.lon, currEvent.timeStamp, 0, false, false, 0, 0, involvedRoadUser, false, ""));
            }
            // Check whether actualZ is one of the top 2 events
            else if (maxZDelta > events.get(4).maxZDelta && enoughTimePassed) {
                Event temp = events.get(4);
                events.set(4, currEvent);
                accEvents.set(4, new IncidentLogEntry(4, currEvent.lat, currEvent.lon, currEvent.timeStamp, 0, false, false, 0, 0, involvedRoadUser, false, ""));
                events.set(5, temp);
                accEvents.set(5, new IncidentLogEntry(5, temp.lat, temp.lon, temp.timeStamp, 0, false, false, 0, 0, involvedRoadUser, false, ""));

            } else if (maxZDelta > events.get(5).maxZDelta && enoughTimePassed) {
                events.set(5, currEvent);
                accEvents.set(5, new IncidentLogEntry(5, currEvent.lat, currEvent.lon, currEvent.timeStamp, 0, false, false, 0, 0, involvedRoadUser, false, ""));
            }
        }

        List<IncidentLogEntry> incidents = new ArrayList<>();
        int key = 0;
        for (IncidentLogEntry incidentLogEntry : accEvents) {
            if (!(incidentLogEntry.latitude == 999 || incidentLogEntry.latitude == 0f)) {
                incidentLogEntry.incidentType = IncidentLogEntry.INCIDENT_TYPE.AUTO_GENERATED;
                incidentLogEntry.key = key++;
                incidents.add(incidentLogEntry);
            }
        }

        return new Pair<>(incidents, 0);
    }

    public static List<DataLogEntry> mergeGPSAndSensor(Queue<DataLogEntry> gpsLines, Queue<DataLogEntry> sensorLines) {
        List<DataLogEntry> dataLogEntries = new ArrayList<>();

        while(!gpsLines.isEmpty() || !sensorLines.isEmpty()) {
            DataLogEntry gpsLine = gpsLines.peek();
            DataLogEntry sensorLine = sensorLines.peek();
            long gpsTS = gpsLine != null ? gpsLine.timestamp : Long.MAX_VALUE;
            long sensorTS = sensorLine != null ? sensorLine.timestamp : Long.MAX_VALUE;
            if (gpsTS <= sensorTS) {
                dataLogEntries.add(gpsLines.poll());
            } else {
                dataLogEntries.add(sensorLines.poll());
            }
        }

        return dataLogEntries;
    }

    /**
     * calculates the nearest three regions to given location
     * @param lat Latitude of current location
     * @param lon Longitude of current location
     * @param context
     * @return int array with the nearest three regions to the location represented by their region IDs
     */
    public static int[] nearestRegionsToThisLocation(double lat, double lon, Context context) {
        int[] result = {Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE};
        double[] top3Distances = {Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE};
        String[] simRa_regions_config = getRegions(context);

        for (int i = 0; i < simRa_regions_config.length; i++) {
            String s = simRa_regions_config[i];
            if (s.split("=").length>3) {
                String latlong = s.split("=")[3];
                double regionLat = Double.parseDouble(latlong.split(",")[0]);
                double regionLon = Double.parseDouble(latlong.split(",")[1]);
                GeoPoint location = new GeoPoint(lat, lon);
                GeoPoint thisRegionCenter = new GeoPoint(regionLat, regionLon);
                double distance = location.distanceToAsDouble(thisRegionCenter);
                if(distance < top3Distances[0]) {
                    top3Distances[2] = top3Distances[1];
                    result[2] = result[1];
                    top3Distances[1] = top3Distances[0];
                    result[1] = result[0];
                    top3Distances[0] = distance;
                    result[0] = i;
                } else if (distance < top3Distances[1]) {
                    top3Distances[2] = top3Distances[1];
                    result[2] = result[1];
                    top3Distances[1] = distance;
                    result[1] = i;
                } else if (distance < top3Distances[2]) {
                    top3Distances[2] = distance;
                    result[2] = i;
                }
            }
        }

        return result;
    }

    /**
     * Converts region IDs to their respective names
     * @param regionCodes int array with the region codes e.g. {2,3,5}
     * @param context context (activity) needed for reading regions file
     * @return string array with the region names of given int array in the same order
     */
    public static String[] regionsDecoder(int[] regionCodes, Context context) {
        String[] result = new String[regionCodes.length];
        String[] region = getRegions(context);
        for (int i = 0; i < regionCodes.length; i++) {
            result[i] = region[regionCodes[i]];
        }
        return result;
    }

    /**
     * Converts names to their respective region IDs
     * @param regionName string array with the region IDs e.g., {Berlin/Potsdam, Leipzig, Stuttgart}
     * @param context context (activity) needed for reading regions file
     * @return int array with the region IDs of given string array in the same order
     */
    public static int regionEncoder(String regionName, Context context) {
        String[] region = getRegions(context);
        for (int i = 0; i < region.length; i++) {
            if (regionName.equals(getCorrectRegionName(region[i]))) {
                return i;
            }
        }
        return -1; // region not found
    }

    /**
     * Gets the correct region names from the region lines of the region file. German or English
     * @param regionLines a subset of getRegions()
     * @return the correct display names in a string array according to System locale.
     */
    public static String[] getCorrectRegionNames(String[] regionLines) {
        String[] result = new String[regionLines.length];
        for (int i = 0; i < regionLines.length; i++) {
            result[i] = getCorrectRegionName(regionLines[i]);
        }
        return result;
    }

    /**
     * Gets the correct region name from the region line of the region file. German or English
     * @param regionLine a region line from getRegions() e.g., Munich=München=München=48.13,11.57
     * @return the correct display name as a string according to System locale
     */
    public static String getCorrectRegionName(String regionLine) {
        String locale = Resources.getSystem().getConfiguration().locale.getLanguage();
        boolean languageIsEnglish = locale.equals(new Locale("en").getLanguage());
        if (languageIsEnglish) {
            return regionLine.split("=")[0];
        } else {
            return regionLine.split("=")[1];
        }
    }

    public static void fireProfileRegionPrompt(int regionsID, Context context) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(context.getString(R.string.chooseRegion));
        CharSequence pleaseChooseRegionText = context.getText(R.string.pleaseChooseRegion);
        CharSequence chosenRegion = getCorrectRegionName(regionsDecoder(new int[]{Profile.loadProfile(null, context).region},context)[0]);

        alert.setMessage( pleaseChooseRegionText + System.lineSeparator() + chosenRegion);
        // alert.setMessage(R.string.pleaseChooseRegion);
        alert.setPositiveButton(R.string.selectRegion, (dialogInterface, j) -> {
            SharedPref.App.News.setLastSeenNewsID(regionsID,context);
            SharedPref.App.RegionsPrompt.setRegionPromptShownAfterV81(true,context);
            startProfileActivityForChooseRegion(context);
        });

        alert.setNeutralButton(R.string.doNotShowAgain, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPref.App.RegionsPrompt.setDoNotShowRegionPrompt(true,context);
                SharedPref.App.RegionsPrompt.setRegionPromptShownAfterV81(true,context);
                SharedPref.App.News.setLastSeenNewsID(regionsID,context);
            }
        });
        alert.setNegativeButton(R.string.later,null);
        alert.show();
    }

    /**
     * checks whether location provider is enabled
     * @param locationManager
     * @return true if the gps provider is disabled, false, if it is enabled
     */
    public static boolean isLocationServiceOff(LocationManager locationManager) {
        boolean gps_enabled = false;
        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            Log.d(TAG, ex.getMessage());
        }
        return (!gps_enabled);
    }

    public static void prepareDebugZipDB(int mode, MetaDataEntry[] rides, Context context) {
        if (mode == 2 || mode == 1) rides = new MetaDataEntry[0];
        zipDb(rides, context);
    }
}
