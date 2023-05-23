package de.tuberlin.mcc.simra.app.entities;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.tuberlin.mcc.simra.app.database.IncidentLogDao;
import de.tuberlin.mcc.simra.app.database.SimRaDB;
import de.tuberlin.mcc.simra.app.util.ResourceUsage;

public class IncidentLog {
    public final static String INCIDENT_LOG_HEADER = "key,lat,lon,ts,bike,childCheckBox,trailerCheckBox,pLoc,incident,i1,i2,i3,i4,i5,i6,i7,i8,i9,scary,desc,i10";
    public final int rideId;
    private TreeMap<Integer, IncidentLogEntry> incidents;
    public int nn_version;
    private static final String TAG = "IncidentLog_LOG";

    public IncidentLog(int rideId, TreeMap<Integer, IncidentLogEntry> incidents, int nn_version) {
        this.rideId = rideId;
        this.incidents = incidents;
        this.nn_version = nn_version;
    }

    /**
     * Merge both IncidentLogs
     *
     * @param primaryIncidentLog   No events will get overwritten
     * @param secondaryIncidentLog Events may get overwritten by primaryIncidentLog
     * @return Merged IncidentLog
     */
    public static IncidentLog mergeIncidentLogs(IncidentLog primaryIncidentLog, IncidentLog secondaryIncidentLog) {
        for (Map.Entry<Integer, IncidentLogEntry> incidentLogEntryEntry : primaryIncidentLog.getIncidents().entrySet()) {
            secondaryIncidentLog.updateOrAddIncident(incidentLogEntryEntry.getValue());
        }
        return secondaryIncidentLog;
    }

    public static IncidentLog loadIncidentLog(int rideId, Context context) {
        return loadIncidentLogWithRideSettingsInformation(rideId, null, null, null, null, context);
    }
    public static IncidentLog loadIncidentLogWithRideSettingsInformation(int rideId, Integer bikeType, Integer phoneLocation, Boolean child, Boolean trailer, Context context) {
        return loadIncidentLogWithRideSettingsAndBoundary(rideId, bikeType, phoneLocation, child, trailer, null, null, context);
    }

    public static IncidentLog loadIncidentLogWithRideSettingsAndBoundary(int rideId, Integer bikeType, Integer phoneLocation, Boolean childOnBoard, Boolean bikeWithTrailer, Long startTimeBoundary, Long endTimeBoundary, Context context) {
        long cpu_start = ResourceUsage.getCpuUtilization();
        TreeMap<Integer, IncidentLogEntry> incidents = new TreeMap() {};

        SimRaDB db = SimRaDB.getDataBase(context);
        IncidentLogDao incidentLogDao = db.getIncidentLogDao();
        IncidentLogEntry[] incidentLogEntries = incidentLogDao.loadIncidentLog(rideId);

        if (incidentLogEntries.length == 0) {
            return new IncidentLog(rideId, incidents, 0);
        }

        for (IncidentLogEntry incidentLogEntry : incidentLogEntries) {
            incidentLogEntry.bikeType = bikeType;
            incidentLogEntry.phoneLocation = phoneLocation;
            incidentLogEntry.childOnBoard = childOnBoard;
            incidentLogEntry.bikeWithTrailer = bikeWithTrailer;
            if (!(incidentLogEntry.incidentType == IncidentLogEntry.INCIDENT_TYPE.FOR_RIDE_SETTINGS) && incidentLogEntry.isInTimeFrame(startTimeBoundary, endTimeBoundary)) {
                incidents.put(incidentLogEntry.key, incidentLogEntry);
            }
        }

        Log.d("RESOURCE", "CPU usage loading IncidentLog: "+(ResourceUsage.getCpuUtilization()-cpu_start));

        return new IncidentLog(rideId, incidents, incidentLogEntries[0].nn_version);
    }

    public static IncidentLog filterIncidentLogTime(IncidentLog incidentLog, Long startTimeBoundary, Long endTimeBoundary) {
        TreeMap<Integer, IncidentLogEntry> incidents = new TreeMap() {};
        for (Map.Entry<Integer, IncidentLogEntry> incidentLogEntry : incidentLog.getIncidents().entrySet()) {
            if (incidentLogEntry.getValue().isInTimeFrame(startTimeBoundary, endTimeBoundary)
            ) {
                incidents.put(incidentLogEntry.getValue().key, incidentLogEntry.getValue());
            }
        }
        return new IncidentLog(incidentLog.rideId, incidents, incidentLog.nn_version);
    }

    public static IncidentLog filterIncidentLogUploadReady(IncidentLog incidentLog, Integer bikeType, Boolean childOnBoard, Boolean bikeWithTrailer, Integer phoneLocation, Boolean forUpload) {

        TreeMap<Integer, IncidentLogEntry> incidents = new TreeMap() {};
        for (Map.Entry<Integer, IncidentLogEntry> incidentLogEntry : incidentLog.getIncidents().entrySet()) {
            Log.d(TAG," for loop 1 incidentLogEntry: " + incidentLogEntry.getValue().stringifyDataLogEntry());
            if (incidentLogEntry.getValue().isReadyForUpload()) {
                Log.d(TAG, "adding incidentLogEntry to incidents");
                IncidentLogEntry thisIncidentLogEntry = incidentLogEntry.getValue();
                thisIncidentLogEntry.bikeType = bikeType;
                thisIncidentLogEntry.phoneLocation = phoneLocation;
                incidentLogEntry.setValue(thisIncidentLogEntry);
                incidents.put(incidentLogEntry.getValue().key, incidentLogEntry.getValue());
            }
        }
        if (incidents.size() == 0) {
            incidents.put(-1,(IncidentLogEntry.newBuilder().withRideInformation(bikeType,childOnBoard,bikeWithTrailer,phoneLocation, IncidentLogEntry.INCIDENT_TYPE.FOR_RIDE_SETTINGS,null,false,null).build()));
        }
        return new IncidentLog(incidentLog.rideId, incidents, incidentLog.nn_version);
    }

    public static void saveIncidentLog(IncidentLog incidentLog, Context context) {
        long cpu_start = ResourceUsage.getCpuUtilization();
        List<IncidentLogEntry> incidents = new ArrayList<>(incidentLog.getIncidents().values());

        //Make sure that all incidents actually have the correct ride-id
        for (int i = 0; i < incidents.size(); i++) {
            IncidentLogEntry incident = incidents.get(i);
            incident.rideId = incidentLog.rideId;
            incident.nn_version = incidentLog.nn_version;
            incidents.set(i, incident);
        }

        SimRaDB.getDataBase(context).getIncidentLogDao().addOrUpdateIncidentLogEntries(incidents);
        Log.d("RESOURCE", "CPU usage saving IncidentLog: "+(ResourceUsage.getCpuUtilization()-cpu_start));
    }

    /**
     * Returns all IncidentLogEntries of a given ride from the db
     * @param rideId the rideId of the given ride
     * @param context
     * @return Array of all relevant IncidentLogEntries
     */
    public static IncidentLogEntry[] loadIncidentLogEntriesOfRide(int rideId, Context context) {
        return SimRaDB.getDataBase(context).getIncidentLogDao().loadIncidentLog(rideId);
    }

    /**
     * Deletes the incidents of a given ride from the db
     * @param rideId the given rideId
     * @param context
     */
    public static void deleteIncidentsOfRide(int rideId, Context context) {
        long cpu_start = ResourceUsage.getCpuUtilization();
        SimRaDB.getDataBase(context).getIncidentLogDao().deleteIncidentLogEntries(rideId);
        Log.d("RESOURCE", "CPU usage deleting IncidentLog: "+(ResourceUsage.getCpuUtilization()-cpu_start));
    }

    public static List<IncidentLogEntry> getScaryIncidents(IncidentLog incidentLog) {
        List<IncidentLogEntry> scaryIncidents = new ArrayList<>();
        for (Map.Entry<Integer, IncidentLogEntry> entry : incidentLog.incidents.entrySet()) {
            if (entry.getValue().scarySituation) {
                scaryIncidents.add(entry.getValue());
            }
        }
        return scaryIncidents;
    }

    public boolean hasAutoGeneratedIncidents() {
        boolean hasAutoGeneratedIncident = false;
        for (Map.Entry<Integer, IncidentLogEntry> incidentLogEntry : this.getIncidents().entrySet()) {
            if (incidentLogEntry.getValue().incidentType == IncidentLogEntry.INCIDENT_TYPE.AUTO_GENERATED) {
                hasAutoGeneratedIncident = true;
            }
        }
        return hasAutoGeneratedIncident;
    }

    public Map<Integer, IncidentLogEntry> getIncidents() {
        return incidents;
    }

    public int getIncidentNumberWithoutRideSettingsIncident() {
        int numberOfIncidentsNotCountingRideSettingsIncident = 0;

        for (Map.Entry<Integer, IncidentLogEntry> incidentLogEntry : this.getIncidents().entrySet()) {
            if (incidentLogEntry.getValue().incidentType != IncidentLogEntry.INCIDENT_TYPE.FOR_RIDE_SETTINGS) {
                numberOfIncidentsNotCountingRideSettingsIncident++;
            }
        }

        return numberOfIncidentsNotCountingRideSettingsIncident;
    }

    public IncidentLogEntry updateOrAddIncident(IncidentLogEntry incidentLogEntry) {
        if (incidentLogEntry.key == null) { // for manually added incidents
            incidentLogEntry.key = (calculateKey(1000));
        } else if (incidentLogEntry.key == 2000) { // for visible OBS incidents
            incidentLogEntry.key = (calculateKey(2000));
        } else if (incidentLogEntry.key == 3000) {// for hidden OBS incidents
            incidentLogEntry.key = (calculateKey(3000));
        }
        incidents.put(incidentLogEntry.key, incidentLogEntry);
        return incidentLogEntry;
    }

    public IncidentLogEntry updateOrAddIncident(IncidentLogEntry incidentLogEntry, int nn_version) {
        this.nn_version = nn_version;
        incidentLogEntry.nn_version = nn_version;
        return updateOrAddIncident(incidentLogEntry);
    }

    public Map<Integer, IncidentLogEntry> removeIncident(IncidentLogEntry incidentLogEntry) {
        incidents.remove(incidentLogEntry.key);
        return incidents;
    }

    public int calculateKey(int key) {
        if (incidents.containsKey(key)) {
            return (calculateKey(key+1));
        } else {
            return key;
        }
    }
}
