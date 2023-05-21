package de.tuberlin.mcc.simra.app.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.databinding.ActivityHistoryBinding;
import de.tuberlin.mcc.simra.app.entities.DataLog;
import de.tuberlin.mcc.simra.app.entities.IncidentLog;
import de.tuberlin.mcc.simra.app.entities.MetaData;
import de.tuberlin.mcc.simra.app.entities.MetaDataEntry;
import de.tuberlin.mcc.simra.app.entities.Profile;
import de.tuberlin.mcc.simra.app.services.UploadService;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import de.tuberlin.mcc.simra.app.util.SharedPref;

import static de.tuberlin.mcc.simra.app.util.SharedPref.lookUpBooleanSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.SharedPref.writeBooleanToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.fireProfileRegionPrompt;

public class HistoryActivity extends BaseActivity {
    private static final String TAG = "HistoryActivity_LOG";
    ActivityHistoryBinding binding;
    boolean exitWhenDone = false;

    String[] ridesArr;
    BroadcastReceiver br;

    public static void startHistoryActivity(Context context) {
        Intent intent = new Intent(context, HistoryActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        //  Toolbar
        setSupportActionBar(binding.toolbar.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        binding.toolbar.toolbar.setTitle("");
        binding.toolbar.toolbar.setSubtitle("");
        binding.toolbar.toolbarTitle.setText(R.string.title_activity_history);

        binding.toolbar.backButton.setOnClickListener(v -> finish());


        binding.listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            LinearLayout historyButtons = binding.buttons;
            boolean isUp = true;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (isUp && view.getLastVisiblePosition() + 1 == totalItemCount) {
                    historyButtons.animate().translationX(historyButtons.getWidth() / 2f);
                    isUp = false;
                } else if (!isUp && !(view.getLastVisiblePosition() + 1 == totalItemCount)) {
                    historyButtons.animate().translationX(0);
                    isUp = true;
                    // historyButtons.setVisibility(View.VISIBLE);
                }
            }
        });

        binding.upload.setOnClickListener(view -> {
            if (!lookUpBooleanSharedPrefs("uploadWarningShown", false, "simraPrefs", HistoryActivity.this)) {
                fireUploadPrompt();
            } else if (Profile.loadProfile(null, HistoryActivity.this).region == 0) {
                fireProfileRegionPrompt(SharedPref.App.Regions.getLastSeenRegionsID(HistoryActivity.this),HistoryActivity.this);
            } else {
                Intent intent = new Intent(HistoryActivity.this, UploadService.class);
                startService(intent);
                Toast.makeText(HistoryActivity.this, getString(R.string.upload_started), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Loads all rides currently saved in the db and shows them in the ArrayAdapter (sorted from
     * newest to oldest)
     */
    private void refreshMyRides() {
        MetaDataEntry[] metaDataEntries = MetaData.getMetadataEntriesSortedByKey(this);

        if (metaDataEntries.length > 0) {
            List<String> stringArrayList = new ArrayList<>();
            for (MetaDataEntry me : metaDataEntries) {
                stringArrayList.add(getRideString(me));
            }

            List<String[]> metaDataLines = new ArrayList<>();
            for (MetaDataEntry entry : metaDataEntries) {
                metaDataLines.add(entry.metaDataEntryToArray());
            }

            MyArrayAdapter myAdapter = new MyArrayAdapter(this, R.layout.row_icons, stringArrayList, metaDataLines);
            binding.listView.setAdapter(myAdapter);
        } else {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator_layout), (getString(R.string.noHistory)), Snackbar.LENGTH_LONG);
            snackbar.show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        br = new MyBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("de.tuberlin.mcc.simra.app.UPLOAD_COMPLETE");
        this.registerReceiver(br, filter);
        refreshMyRides();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(br);
    }

    /**
     * Create text representation via the metadata of a ride
     *
     * @param entry the metadata entry for the ride
     * @return the string with ride-info
     */
    private String getRideString(MetaDataEntry entry) {
        String todo = getString(R.string.newRideInHistoryActivity);

        if (entry.state == 1) {
            todo = getString(R.string.rideAnnotatedInHistoryActivity);
        } else if (entry.state == 2) {
            todo = getString(R.string.rideUploadedInHistoryActivity);
        }

        int minutes = Math.round(((entry.endTime - entry.startTime) / 1000 / 60));
        Date dt = new Date(entry.startTime);
        Calendar localCalendar = Calendar.getInstance(TimeZone.getDefault());
        localCalendar.setTime(dt);
        Locale locale = Resources.getSystem().getConfiguration().locale;

        SimpleDateFormat wholeDateFormat = new SimpleDateFormat(getString(R.string.datetime_format), locale);
        String datetime = wholeDateFormat.format(dt);

        return "#" + entry.rideId + ";" + datetime + ";" + todo + ";" + minutes + ";" + entry.state + ";" + Objects.requireNonNullElse(entry.distance, 0);
    }

    public void fireDeletePrompt(int position, MyArrayAdapter arrayAdapter) {
        AlertDialog.Builder alert = new AlertDialog.Builder(HistoryActivity.this);
        alert.setTitle(getString(R.string.warning));
        alert.setMessage(getString(R.string.delete_file_warning));
        alert.setPositiveButton(R.string.delete_ride_approve, (dialog, id) -> {
            String clicked = (String) binding.listView.getItemAtPosition(position);
            Log.d(TAG, "btnDelete.onClick() clicked: " + clicked);
            clicked = clicked.replace("#", "").split(";")[0];

            int rideId = Integer.parseInt(clicked);

            MetaData.deleteMetadataEntryForRide(rideId, this);

            DataLog.deleteEntriesOfRide(rideId, this);

            IncidentLog.deleteIncidentsOfRide(rideId, this);

            Toast.makeText(HistoryActivity.this, R.string.ride_deleted, Toast.LENGTH_SHORT).show();
            refreshMyRides();
        });
        alert.setNegativeButton(R.string.cancel, (dialog, id) -> {
        });
        alert.show();

    }

    public void fireUploadPrompt() {
        AlertDialog.Builder alert = new AlertDialog.Builder(HistoryActivity.this);
        alert.setTitle(getString(R.string.warning));
        alert.setMessage(getString(R.string.upload_file_warning));
        alert.setPositiveButton(R.string.upload, (dialog, id) -> {
            if (Profile.loadProfile(null, HistoryActivity.this).region == 0) {
                fireProfileRegionPrompt(SharedPref.App.Regions.getLastSeenRegionsID(HistoryActivity.this),HistoryActivity.this);
            } else {
                writeBooleanToSharedPrefs("uploadWarningShown", true, "simraPrefs", HistoryActivity.this);
                Intent intent = new Intent(HistoryActivity.this, UploadService.class);
                startService(intent);
                Toast.makeText(HistoryActivity.this, getString(R.string.upload_started), Toast.LENGTH_SHORT).show();
                if (exitWhenDone) {
                    HistoryActivity.this.moveTaskToBack(true);
                }
            }
        });
        alert.setNegativeButton(R.string.cancel, (dialog, id) -> {
        });
        alert.show();
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean uploadSuccessful = intent.getBooleanExtra("uploadSuccessful", false);
            boolean foundARideToUpload = intent.getBooleanExtra("foundARideToUpload", true);
            if (!foundARideToUpload) {
                Toast.makeText(getApplicationContext(), R.string.nothing_to_upload, Toast.LENGTH_LONG).show();
            } else if (!uploadSuccessful) {
                Toast.makeText(getApplicationContext(), R.string.upload_failed, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.upload_completed, Toast.LENGTH_LONG).show();
            }

            refreshMyRides();
        }
    }

    public class MyArrayAdapter extends ArrayAdapter<String> {
        String TAG = "MyArrayAdapter_LOG";

        Context context;
        int layoutResourceId;
        List<String> stringList;
        List<String[]> metaDataLines;

        public MyArrayAdapter(Context context, int layoutResourceId, List<String> stringList, List<String[]> metaDataLines) {

            super(context, layoutResourceId, stringList);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.stringList = stringList;
            this.metaDataLines = metaDataLines;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            Holder holder = null;

            if (row == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);
                holder = new Holder();
                holder.rideDate = row.findViewById(R.id.row_icons_ride_date);
                holder.rideTime = row.findViewById(R.id.row_ride_time);
                holder.duration = row.findViewById(R.id.row_duration);
                holder.distance = row.findViewById(R.id.row_distance);
                holder.distanceUnit = row.findViewById(R.id.row_distanceKM);
                holder.status = row.findViewById(R.id.statusBtn);
                holder.btnDelete = row.findViewById(R.id.deleteBtn);
                row.setTag(holder);
            } else {
                holder = (Holder) row.getTag();
            }
            String[] itemComponents = stringList.get(position).split(";");
            holder.rideDate.setText(itemComponents[1].split(",")[0]);
            holder.rideTime.setText(itemComponents[1].split(",")[1]);
            // holder.message.setText(itemComponents[2]);
            Log.d(TAG, "itemComponents: " + Arrays.toString(itemComponents));

            if (itemComponents[2].contains(getString(R.string.rideAnnotatedInHistoryActivity))) {
                holder.status.setBackground(getDrawable(R.drawable.ic_phone_android_black_24dp));
            } else if (itemComponents[2].contains(getString(R.string.rideUploadedInHistoryActivity))) {
                holder.status.setBackground(getDrawable(R.drawable.ic_cloud_done_black_24dp));
            } else {
                holder.status.setBackground(null);
            }
            holder.duration.setText(itemComponents[3]);
            if (SharedPref.Settings.DisplayUnit.isImperial(HistoryActivity.this)) {
                holder.distance.setText(String.valueOf(Math.round(((Double.parseDouble(itemComponents[5]) / 1600) * 100.0)) / 100.0));
                holder.distanceUnit.setText("mi");
            } else {
                holder.distance.setText(String.valueOf(Math.round(((Double.parseDouble(itemComponents[5]) / 1000) * 100.0)) / 100.0));
                holder.distanceUnit.setText("km");
            }
            if (!itemComponents[4].equals("2")) {
                holder.btnDelete.setVisibility(View.VISIBLE);
            } else {
                holder.btnDelete.setVisibility(View.INVISIBLE);
            }
            row.setOnClickListener(v -> {
                String clicked = (String) binding.listView.getItemAtPosition(position);
                int rideID = Integer.parseInt(clicked.replace("#", "").split(";")[0]);

                MetaDataEntry entry = MetaData.getMetadataEntryForRide(rideID, HistoryActivity.this);
                ShowRouteActivity.startShowRouteActivity(rideID, entry.state, true, HistoryActivity.this);
            });

            holder.btnDelete.setOnClickListener(v -> {
                Log.d(TAG, "Delete Button Clicked");
                fireDeletePrompt(position, MyArrayAdapter.this);
            });
            return row;
        }

        class Holder {
            TextView rideDate;
            TextView rideTime;
            TextView duration;
            TextView distance;
            TextView distanceUnit;
            ImageButton status;
            ImageButton btnDelete;
        }
    }
}
