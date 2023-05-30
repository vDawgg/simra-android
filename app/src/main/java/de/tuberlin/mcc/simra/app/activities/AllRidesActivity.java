package de.tuberlin.mcc.simra.app.activities;

import android.os.AsyncTask;

import org.osmdroid.views.overlay.Polyline;

import java.util.Arrays;
import java.util.List;

import de.tuberlin.mcc.simra.app.database.SimRaDB;
import de.tuberlin.mcc.simra.app.entities.DataLog;
import de.tuberlin.mcc.simra.app.entities.DataLogEntry;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import de.tuberlin.mcc.simra.app.entities.MetaData;
import de.tuberlin.mcc.simra.app.entities.MetaDataEntry;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import kotlin.Pair;

public class AllRidesActivity extends BaseActivity {



    private class LoadDataLogsTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            for (MetaDataEntry me : MetaData.getMetadataEntriesSortedByKey(AllRidesActivity.this)) {
                Pair<DataLogEntry[], IncidentLogEntry[]> p = SimRaDB.getDataBase(AllRidesActivity.this).getCombinedDao().readDataAndIncidents(me.rideId);

                List<DataLogEntry> onlyGPS = DataLog.getGPSDataLogEntries(p.getFirst());
                DataLog originalDataLog = new DataLog(p.getFirst()[0].rideId, Arrays.asList(p.getFirst()), onlyGPS, DataLog.RideAnalysisData.calculateRideAnalysisData(onlyGPS), p.getFirst()[0].timestamp, p.getFirst()[p.getFirst().length-1].timestamp);
                Polyline route = originalDataLog.rideAnalysisData.route;


            }
            return null;
        }
    }
}
