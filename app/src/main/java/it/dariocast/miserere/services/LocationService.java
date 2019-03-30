package it.dariocast.miserere.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import it.dariocast.miserere.R;
import it.dariocast.miserere.classi.Constants;
import it.dariocast.miserere.classi.Coordinate;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

public class LocationService extends Service {
    private final static String TAG = "LocationService";
    private LocationListener mLocationListener;
    private LocationManager mLocationManager;
    private NotificationManager notificationManager;
    private int idConfraternita;

    public LocationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Constants.ACTION.STARTTRACKING_ACTION)) {
            Log.i(TAG, "Ricevuto l'intent di inizio trasmissione");
            idConfraternita = intent.getIntExtra("id",1);

            String NOTIFICATION_CHANNEL_ID = "it.dariocast.miserere";
            Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle("Miserere")
                    .setTicker("Miserere")
                    .setContentText("Trasmissione della posizione attivata")
                    .setSmallIcon(R.drawable.black_person)
                    .setOngoing(true)
                    .build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String channelName = "Miserere Background Service";
                NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
                chan.setLightColor(Color.GRAY);
                chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                assert manager != null;
                manager.createNotificationChannel(chan);

                startTracking();
                startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notification);
            } else {
                stopTracking();
                startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notification);
            }
        } else if (intent.getAction().equals(
                Constants.ACTION.STOPTRACKING_ACTION)) {
            Log.i(TAG, "Ricevuto l'intent di fine trasmissione");
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationManager != null) {
            try {
                mLocationManager.removeUpdates(mLocationListener);
            } catch (Exception ex) {
                Log.i(TAG, "fail to remove location listners, ignore", ex);
            }
        }
    }

    //LocationListener per le coordinate
    private class LocationListener implements android.location.LocationListener {
        private Location lastLocation = null;
        private final String TAG = "LocationListener";
        private Location mLastLocation;

        public LocationListener(String provider) {
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            mLastLocation = location;
            Log.i(TAG, "LocationChanged: " + location.toString());
            new PostCoordTask().execute(new Coordinate(idConfraternita, mLastLocation.getLatitude(), mLastLocation.getLongitude(), "testa"));
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.i(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.i(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.i(TAG, "onStatusChanged: " + status);
        }
    }

    private void initializeLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    public void startTracking() {
        initializeLocationManager();
        mLocationListener = new LocationListener(LocationManager.GPS_PROVIDER);

        try {
            int LOCATION_INTERVAL = 500;
            int LOCATION_DISTANCE = 10;
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListener);

        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }

    }

    public void stopTracking() {
        this.onDestroy();
    }
}

class PostCoordTask extends AsyncTask<Coordinate, Void, Void> {
    private static final String TAG = "PostCoordTask";
    private static final String coordEndpoint = "https://dariocast.altervista.org/miserere/api/coordinate.php";

    PostCoordTask() {
    }

    @Override
    protected Void doInBackground(Coordinate... coordinates) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject obj = new JSONObject();
        try {
            obj.put("onfraternita", coordinates[0].confraternitaId);
            obj.put("lat", coordinates[0].lat+"");
            obj.put("lon", coordinates[0].lon+"");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(JSON, obj.toString());
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(coordEndpoint)
                .post(body)
                .build();
        Log.d(TAG, "RequestBody = " + obj.toString());
        try (Response response = client.newCall(request).execute()) {
            Log.d(TAG, "Post le nuove coordinate, response code: " + response.code());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
