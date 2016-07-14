package ee.jdog.asukohaabimees;

import android.support.v7.app.AppCompatActivity;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import ee.jdog.asukohaabimees.R;
import ee.jdog.asukohaabimees.service.LocationService;


/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = MainActivity.class.getSimpleName();

    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buildGoogleApiClient();

        initUI();
    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }


    private void initUI() {

        final EditText urlInput = (EditText)findViewById(R.id.input_url);
        Button saveButton = (Button)findViewById(R.id.btn_save);
        SeekBar intervalSeekbar = (SeekBar)findViewById(R.id.seekbar_interval);
        final TextView intervalText = (TextView)findViewById(R.id.text_interval);
        Button startButton = (Button)findViewById(R.id.btn_start);
        Button stopButton = (Button)findViewById(R.id.btn_stop);

        SharedPreferences sharedPref = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
        String url = sharedPref.getString("url", null);
        if (url != null) {
            urlInput.setText(url);
        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = urlInput.getText().toString();

                SharedPreferences sharedPref = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("url", url);
                editor.commit();

                // restart service
                restartLocationUpdates();
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLocationCallbacks();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopLocationUpdates();
            }
        });

        final int interval = sharedPref.getInt("interval", 1);
        intervalText.setText(Integer.toString(interval) + " s");
        intervalSeekbar.setProgress(interval - 1);

        intervalSeekbar.setMax(59);
        intervalSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                intervalText.setText(Integer.toString(progress + 1) + " s");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SharedPreferences sharedPref =
                        getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("interval", seekBar.getProgress() + 1);
                editor.commit();

                // restart service
                restartLocationUpdates();
            }
        });
    }

    // start location updates and direct them to background service
    private void startLocationCallbacks() {

        SharedPreferences sharedPref = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
        int interval = sharedPref.getInt("interval", 1);

        LocationRequest locationRequest = new LocationRequest()
                .setInterval(interval * 1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        Intent intent = new Intent(this, LocationService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 1, intent, 0);

        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient, locationRequest, pendingIntent);
        } catch (SecurityException se) {

        }
    }

    // stop location updates and terminate service
    private void stopLocationUpdates() {

        Intent intent = new Intent(this, LocationService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 1, intent, 0);

        LocationServices.FusedLocationApi
                .removeLocationUpdates(googleApiClient, pendingIntent)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // stop the service
                        Intent intent = new Intent(MainActivity.this, LocationService.class);
                        stopService(intent);
                    }
                });
    }

    // stop location updates and terminate service
    private void restartLocationUpdates() {

        Intent intent = new Intent(this, LocationService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 1, intent, 0);

        LocationServices.FusedLocationApi
                .removeLocationUpdates(googleApiClient, pendingIntent)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // stop the service
                        Intent intent = new Intent(MainActivity.this, LocationService.class);
                        stopService(intent);

                        startLocationCallbacks();
                    }
                });
    }


    // Play Services API stuff
    protected void buildGoogleApiClient() {

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .enableAutoManage(this, this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationCallbacks();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // nop
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Asukoha pärimine ebaõnnestus! "
                + connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }
}
