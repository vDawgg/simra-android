package de.tuberlin.mcc.simra.app.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "metadata_table")
public class MetaDataEntry {
    //TODO: Find out if this really should be the prim-key
    @PrimaryKey
    @NonNull
    public Integer rideId;
    public Long startTime;
    public Long endTime;
    public Integer state;
    public Integer numberOfIncidents;
    public Long waitedTime;
    public Long distance;
    public Integer numberOfScaryIncidents;
    public Integer region;
    public Long lastModified;

    public MetaDataEntry(@NonNull Integer rideId, Long startTime, Long endTime, Integer state, Integer numberOfIncidents, Long waitedTime, Long distance, Integer numberOfScaryIncidents, Integer region, Long lastModified) {
        this.rideId = rideId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.state = state != null ? state : MetaData.STATE.JUST_RECORDED;
        this.numberOfIncidents = numberOfIncidents != null ? numberOfIncidents : 0;
        this.waitedTime = waitedTime != null ? waitedTime : 0;
        this.distance = distance != null ? distance : 0;
        this.numberOfScaryIncidents = numberOfScaryIncidents != null ? numberOfScaryIncidents : 0;
        this.region = region != null ? region : 0;
        this.lastModified = lastModified;
    }

    public static MetaDataEntry parseEntryFromLine(String string) {
        String[] dataLogLine = string.split(",", -1);
        return new MetaDataEntry(
                Integer.parseInt(dataLogLine[0]),
                Long.parseLong(dataLogLine[1]),
                Long.parseLong(dataLogLine[2]),
                Integer.parseInt(dataLogLine[3]),
                Integer.parseInt(dataLogLine[4]),
                Long.parseLong(dataLogLine[5]),
                Long.parseLong(dataLogLine[6]),
                Integer.parseInt(dataLogLine[7]),
                Integer.parseInt(dataLogLine[8]),
                System.currentTimeMillis()
        );
    }


    //This is still needed for the upload-task
    /**
     * Stringifies the MetaDataEntry Object to a CSV Log Line
     *
     * @return Log Line without new line separator
     */
    public String stringifyMetaDataEntry() {
        return rideId + "," + startTime + "," + endTime + "," + state + "," + numberOfIncidents + "," + waitedTime + "," + distance + "," + numberOfScaryIncidents + "," + region;
    }

    //TODO: Check if this could/should be an override!
    public String[] metaDataEntryToArray() {
        String[] s = new String[9];
        s[0] = rideId.toString();
        s[1] = startTime.toString();
        s[2] = endTime.toString();
        s[3] = state.toString();
        s[4] = numberOfIncidents.toString();
        s[5] = waitedTime.toString();
        s[6] = distance.toString();
        s[7] = numberOfIncidents.toString();
        s[8] = region.toString();
        return s;
    }
}
