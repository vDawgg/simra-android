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
import androidx.core.content.res.ResourcesCompat;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.IconOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.annotation.MyInfoWindow;
import de.tuberlin.mcc.simra.app.annotation.RideInfoWindow;
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
        MetaDataEntry[] metaDataEntries = MetaData.getMetadataEntriesSortedByKey(this);
        List<GeoPoint> middleLocations = new ArrayList<>();
        for (MetaDataEntry me : metaDataEntries) {
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
            DataLogEntry middleEntry = onlyGPS.get(onlyGPS.size()/2);
            middleLocations.add(new GeoPoint(middleEntry.latitude, middleEntry.longitude));

            IncidentLog incidentLog = IncidentLog.makeIncidentLog(p.getSecond(), me.rideId);
            Polyline route = dataLog.rideAnalysisData.route;

            route.getOutlinePaint().setStrokeWidth(40f);
            route.getOutlinePaint().setColor(getColorBasedOnIncidents(me));
            route.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
            binding.allRidesMap.getOverlayManager().add(route);

            routes.add(route);

            MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
                @Override
                public boolean singleTapConfirmedHelper(GeoPoint p) {
                    InfoWindow.closeAllInfoWindowsOn(binding.allRidesMap);
                    return true;
                }

                @Override
                public boolean longPressHelper(GeoPoint p) {
                    return false;
                }
            };
            MapEventsOverlay overlayEvents = new MapEventsOverlay(getBaseContext(), mapEventsReceiver);
            runOnUiThread(() -> {
                binding.allRidesMap.getOverlays().add(overlayEvents);
                binding.allRidesMap.invalidate();
            });
        }

        runOnUiThread(() -> {
            for (int i = 0; i < metaDataEntries.length; i++) {
                routes.get(i).getOutlinePaint().setStrokeWidth(8f);
                routes.get(i).getOutlinePaint().setColor(getColor(R.color.colorPrimaryDark));
                routes.get(i).getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
                binding.allRidesMap.getOverlayManager().add(routes.get(i));

                Drawable startFlag = AllRidesActivity.this.getResources().getDrawable(R.drawable.startblack, null);
                Drawable finishFlag = AllRidesActivity.this.getResources().getDrawable(R.drawable.racingflagblack, null);
                GeoPoint startFlagPoint = routes.get(i).getPoints().get(0);
                GeoPoint finishFlagPoint = routes.get(i).getPoints().get(routes.get(i).getPoints().size() - 1);

                IconOverlay startFlagOverlay = new IconOverlay(startFlagPoint, startFlag);
                IconOverlay finishFlagOverlay = new IconOverlay(finishFlagPoint, finishFlag);
                binding.allRidesMap.getOverlays().add(startFlagOverlay);
                binding.allRidesMap.getOverlays().add(finishFlagOverlay);

                Marker marker = new Marker(binding.allRidesMap);
                marker.setPosition(middleLocations.get(i));
                marker.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.pin, null));
                InfoWindow infoWindow = new RideInfoWindow(R.layout.incident_bubble, binding.allRidesMap, metaDataEntries[i]);
                marker.setInfoWindow(infoWindow);
                binding.allRidesMap.getOverlays().add(marker);
                binding.allRidesMap.invalidate();
            }
        });

        runOnUiThread(() -> {
            if (routes.size() > 0) {
                BoundingBox bBox = getBoundingBox(routes);
                zoomToBBox(bBox);
            }
            binding.allRidesMap.invalidate();
        });
    }

    //TODO: Work out if this is fine with Ahmet
    private int getColorBasedOnIncidents(MetaDataEntry metaDataEntry) {
        if (metaDataEntry.numberOfIncidents > 10 || metaDataEntry.numberOfScaryIncidents > 2) {
            return getColor(R.color.distanceMarkerDanger);
        } else if (metaDataEntry.numberOfIncidents > 5 || metaDataEntry.numberOfScaryIncidents >= 1) {
            return getColor(R.color.distanceMarkerWarning);
        } else {
            return getColor(R.color.colorPrimaryDark);
        }
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
