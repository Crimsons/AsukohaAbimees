package ee.jdog.asukohaabimees.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderApi;

import ee.jdog.asukohaabimees.MainActivity;
import ee.jdog.asukohaabimees.R;
import ee.jdog.asukohaabimees.network.Api;
import ee.jdog.asukohaabimees.network.LocationRequest;
import ee.jdog.asukohaabimees.network.SetLocationResponse;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Jakob on 07/14/16.
 */
public class LocationService extends Service {

    private static final String TAG = "LocationService";

    private ServiceHandler mServiceHandler;
    private Api api;


    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            Location location = msg.getData().getParcelable("location");

            if ( location == null )
                return;

            Log.e(TAG, "location: " + location.toString());

            if (api != null) {
                Call<SetLocationResponse> call =
                        api.sendLocation(new LocationRequest(location.getLatitude(), location.getLongitude(), location.getSpeed()));

                call.enqueue(new retrofit2.Callback<SetLocationResponse>() {
                    @Override
                    public void onResponse(Call<SetLocationResponse> call, Response<SetLocationResponse> response) {
                        Log.e(TAG, response.message());
                    }

                    @Override
                    public void onFailure(Call<SetLocationResponse> call, Throwable t) {
                        t.printStackTrace();
                    }
                });
            }
        }
    }

    @Override
    public void onCreate() {

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // initialize api client
        SharedPreferences sharedPref = getApplicationContext()
                .getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
        String url = sharedPref.getString("url", null);

        if (url != null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            api = retrofit.create(Api.class);
        }

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceHandler = new ServiceHandler(thread.getLooper());

        // set as foreground service
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(largeIcon)
                .setContentIntent(pendingIntent);
        startForeground(1, notificationBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Location location =
                intent.getParcelableExtra(FusedLocationProviderApi.KEY_LOCATION_CHANGED);
        Bundle data = new Bundle();
        data.putParcelable("location", location);
        Message msg = mServiceHandler.obtainMessage();
        msg.setData(data);
        mServiceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

}
