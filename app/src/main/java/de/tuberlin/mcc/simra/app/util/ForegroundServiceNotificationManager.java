package de.tuberlin.mcc.simra.app.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.activities.MainActivity;

public class ForegroundServiceNotificationManager {

    private static final int NOTIFICATION_ID = 1094;
    private static final String CHANNEL_ID = "ForegroundServiceNotificationChannel";

    public static Notification createOrUpdateNotification(Context ctx, String title, String text) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(ctx);
        Intent mainActivityIntent = new Intent(ctx, MainActivity.class);
        mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE);

        // From API 26+ onwards a NotificationChannel has to be created
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Ride Information", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("SimRa Channel");
            notificationManager.createNotificationChannel(channel);
        }

        // Set the intent that will fire when the user taps the notification
        Notification notification = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.simra_logo)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_bluetooth, ctx.getResources().getString(R.string.foregroundNotificationButtonBack),
                        pendingIntent)
                .build();
        notificationManager.notify(NOTIFICATION_ID, notification);

        return notification;
    }

    public static int getNotificationId() {
        return NOTIFICATION_ID;
    }

    public static void cancelNotification(Context ctx) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(ctx);
        notificationManager.cancel(NOTIFICATION_ID);
    }
}
