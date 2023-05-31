package de.tuberlin.mcc.simra.app.annotation;

import android.widget.LinearLayout;
import android.widget.TextView;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.activities.HistoryActivity;
import de.tuberlin.mcc.simra.app.entities.MetaDataEntry;

public class RideInfoWindow extends InfoWindow {
    MetaDataEntry me;

    public RideInfoWindow(int layoutResId, MapView mapView, MetaDataEntry me) {
        super(layoutResId, mapView);
        this.me = me;
    }

    @Override
    public void onOpen(Object item) {
        TextView txtTitle = mView.findViewById(R.id.bubble_title);
        TextView txtDescription = mView.findViewById(R.id.bubble_description);

        //TODO: Add to strings
        txtTitle.setText("Fahrtinfo");
        txtDescription.setText(buildInfo(me));
    }

    @Override
    public void onClose() {

    }

    private String buildInfo(MetaDataEntry me) {
        return "Duration: " + Math.round(me.endTime - me.startTime) / 100 / 60 + System.lineSeparator() +
                "Distance: " + me.distance + System.lineSeparator() +
                "Number of Incidents: " + me.numberOfIncidents + System.lineSeparator() +
                "Number of scary Incidents: " + me.numberOfScaryIncidents;
    }
}
