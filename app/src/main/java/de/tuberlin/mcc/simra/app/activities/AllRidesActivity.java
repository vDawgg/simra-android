package de.tuberlin.mcc.simra.app.activities;

import static de.tuberlin.mcc.simra.app.util.Constants.ZOOM_LEVEL;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.IconOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.database.SimRaDB;
import de.tuberlin.mcc.simra.app.databinding.ActivityAllRidesBinding;
import de.tuberlin.mcc.simra.app.databinding.ActivityShowRouteBinding;
import de.tuberlin.mcc.simra.app.entities.DataLog;
import de.tuberlin.mcc.simra.app.entities.DataLogEntry;
import de.tuberlin.mcc.simra.app.entities.IncidentLog;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import de.tuberlin.mcc.simra.app.entities.MetaData;
import de.tuberlin.mcc.simra.app.entities.MetaDataEntry;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import kotlin.Pair;

public class AllRidesActivity extends BaseActivity {
    ActivityAllRidesBinding binding;
    List<Polyline> routes = new ArrayList<>();

    public MapView getmMapView() {
        return binding.allRidesMap;
    }

    /**
     * Zoom automatically to the bounding box.
     *
     * @param bBox
     */
    public void zoomToBBox(BoundingBox bBox) {
        // Usually the command in the if body should suffice
        // but osmdroid is buggy and we need the else part to fix it.
        MapView mMapView = binding.allRidesMap;
        if ((mMapView.getIntrinsicScreenRect(null).bottom - mMapView.getIntrinsicScreenRect(null).top) > 0) {
            mMapView.zoomToBoundingBox(bBox, false);
        } else {
            ViewTreeObserver vto = mMapView.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                @Override
                public void onGlobalLayout() {
                    mMapView.zoomToBoundingBox(bBox, false);
                    ViewTreeObserver vto2 = mMapView.getViewTreeObserver();
                    vto2.removeOnGlobalLayoutListener(this);
                }
            });
        }

        mMapView.setMinZoomLevel(7.0);
        if (mMapView.getMaxZoomLevel() > 19.0) {
            mMapView.setMaxZoomLevel(19.0);
        }
    }

    /**
     * Similar to ShowRouteActivity.getBoundingBox(), but returns BoundingBox for all rides
     *
     * @param pls PolyLine list of all rides
     * @return BoundingBox that encapsulates the PolyLines of all rides
     */
    private static BoundingBox getBoundingBox(List<Polyline> pls) {
        List<GeoPoint> tempPoints = pls.get(0).getActualPoints();
        double[] border = {tempPoints.get(0).getLatitude(), tempPoints.get(0).getLongitude(), tempPoints.get(0).getLatitude(), tempPoints.get(0).getLongitude()};

        for (Polyline pl : pls) {
            // {North, East, South, West}
            List<GeoPoint> geoPoints = pl.getActualPoints();

            for (int i = 0; i < geoPoints.size(); i++) {
                // Check for south/north
                if (geoPoints.get(i).getLatitude() < border[2]) {
                    border[2] = geoPoints.get(i).getLatitude();
                }
                if (geoPoints.get(i).getLatitude() > border[0]) {
                    border[0] = geoPoints.get(i).getLatitude();
                }
                // Check for west/east
                if (geoPoints.get(i).getLongitude() < border[3]) {
                    border[3] = geoPoints.get(i).getLongitude();
                }
                if (geoPoints.get(i).getLongitude() > border[1]) {
                    border[1] = geoPoints.get(i).getLongitude();
                }
            }
        }
        return new BoundingBox(border[0] + 0.001, border[1] + 0.001, border[2] - 0.001, border[3] - 0.001);
    }

    //TODO: Add a popup if no rides have been recorded yet.
    private void showRoute() {
        for (MetaDataEntry me : MetaData.getMetadataEntriesSortedByKey(this)) {
            Pair<DataLogEntry[], IncidentLogEntry[]> p = SimRaDB
                    .getDataBase(this)
                    .getCombinedDao()
                    .readAll(me.rideId);

            List<DataLogEntry> onlyGPS = DataLog.getGPSDataLogEntries(p.getFirst());
            DataLog dataLog = new DataLog(p.getFirst()[0].rideId,
                    Arrays.asList(p.getFirst()),
                    onlyGPS,
                    DataLog.RideAnalysisData.calculateRideAnalysisData(onlyGPS),
                    p.getFirst()[0].timestamp,
                    p.getFirst()[p.getFirst().length-1].timestamp);

            IncidentLog incidentLog = IncidentLog.makeIncidentLog(p.getSecond(), me.rideId);

            // Start with thick outline
            Polyline route = dataLog.rideAnalysisData.route;
            route.getOutlinePaint().setStrokeWidth(40f);
            //TODO: Alternate between colors
            route.getOutlinePaint().setColor(getColor(R.color.colorPrimaryDark));
            route.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
            binding.allRidesMap.getOverlayManager().add(route);

            //TODO: Change this, as only one of the two polylines is showing (the smaller one below)

            runOnUiThread(() -> {
                Drawable startFlag = AllRidesActivity.this.getResources().getDrawable(R.drawable.startblack, null);
                Drawable finishFlag = AllRidesActivity.this.getResources().getDrawable(R.drawable.racingflagblack, null);
                GeoPoint startFlagPoint = dataLog.rideAnalysisData.route.getPoints().get(0);
                GeoPoint finishFlagPoint = dataLog.rideAnalysisData.route.getPoints().get(dataLog.rideAnalysisData.route.getPoints().size() - 1);

                IconOverlay startFlagOverlay = new IconOverlay(startFlagPoint, startFlag);
                IconOverlay finishFlagOverlay = new IconOverlay(finishFlagPoint, finishFlag);
                binding.allRidesMap.getOverlays().add(startFlagOverlay);
                binding.allRidesMap.getOverlays().add(finishFlagOverlay);

                Polyline route2 = dataLog.rideAnalysisData.route;
                route2.getOutlinePaint().setStrokeWidth(8f);
                route2.getOutlinePaint().setColor(getColor(R.color.colorPrimary));
                route2.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
                binding.allRidesMap.getOverlays().add(route2);
            });

            routes.add(route);
        }

        runOnUiThread(() -> {
            if (routes.size() > 0) {
                BoundingBox bBox = getBoundingBox(routes);
                zoomToBBox(bBox);
            }
            Log.d("DEBUG", "Here!");
            binding.allRidesMap.invalidate();
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAllRidesBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar.toolbar;
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        TextView toolbarTxt = binding.toolbar.toolbarTitle;
        //TODO: Add this to strings.xml
        toolbarTxt.setText("Alle Fahrten");
        binding.toolbar.backButton.setOnClickListener(v -> finish());

        MapView mMapView = binding.allRidesMap;
        mMapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mMapView.setMultiTouchControls(true); // gesture zooming
        mMapView.setFlingEnabled(true);
        MapController mMapController = (MapController) mMapView.getController();
        mMapController.setZoom(ZOOM_LEVEL);
        binding.copyrightText.setMovementMethod(LinkMovementMethod.getInstance());

        new ShowRideTask().execute();

        // scales tiles to dpi of current display
        mMapView.setTilesScaledToDpi(true);
    }

    private class ShowRideTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            showRoute();
            return null;
        }
    }
}
