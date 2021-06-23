// final consistent location tracker
package com.example.currentlocation;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;

public class LocationService extends Service
{
    private String TAG = LocationService.class.getSimpleName(); // the simple name of the class (for logs)
    private static final String CHANNEL_ID = "location_notification_channel"; // notification channel id
    private static final String CHANNEL_NAME = "Location Service"; // The user visible name of the notification channel
    private static final String CHANNEL_DESCRIPTION = "This channel is used by location service"; // notification channel description
    private static final int REQUEST_CODE = 0; // private request code for the sender of the pending intent
    public static final int DEFAULT_INTERVAL = 7000; // default location update interval
    public static final int FASTEST_INTERVAL = 4000; // fastest location update interval
    private LocationCallback locationCallBack ; //Used for receiving notifications from the FusedLocationProviderApi
                                                // when the device location has changed or can no longer be determined
    private Location location; // last known location or updated location
    private LocationRequest locationRequest; // contains parameters for the location request
    private SharedPreferences mPreferences; // Interface for accessing and modifying preference data

    // triggered when starting the service (every single time)

    @Override
    public void onCreate() {
        super.onCreate();
        locationCallBack = new LocationCallback()
        {
            //event that is triggered whenever the update interval is met
            @Override
            public void onLocationResult(LocationResult locationResult)
            {
                Log.d("Location update", "*****onLocationResult, before if****");
                super.onLocationResult(locationResult);
                // get username from shared preferences
                String username = get_username();
                // if the location is available
                if(locationResult!=null && locationResult.getLastLocation()!=null)
                {
                    //handle location result
                    location = locationResult.getLastLocation();
                    double  latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    Log.d(TAG ,latitude + ", " + longitude + ", " + getUserAddress(latitude, longitude));
                    try {
                        SockMngr.sendAndReceive(username + "," +location.getLatitude() + "," + location.getLongitude());
                        Log.d(TAG, SockMngr.response);
                        // if an alert is received
                        if(SockMngr.response.equals("CODE RED"))
                        {
                            Log.d(TAG, "EXPOSED");
                            // pop notification
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_baseline_error_24)
                                    .setContentTitle("ALERT")
                                    .setContentText("You are exposed to a person with Covid-19 ")
                                    .setAutoCancel(true)
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                            notificationManager.notify(1111, builder.build());
                        }
                        else if(SockMngr.response.startsWith("HISTORY_CHECK_ALERT"))
                        {
                            Log.d(TAG, "*****HISTORY_CHECK_ALERT*****");
                            String [] arr = SockMngr.response.split(",");
                            double alert_lat = Double.parseDouble(arr[1]);
                            double alert_long = Double.parseDouble(arr[2]);
                            String time = arr[3];
                            String date = arr[4];
                            String place = getUserAddress(alert_lat, alert_long);
                            String msg = "You have been exposed to a Covid-19 carrier on " + date + " at " + time + " on " + place;
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_baseline_error_24)
                                    .setContentTitle("You have been exposed to a carrier")
                                    .setContentText(msg)
                                    .setStyle(new NotificationCompat.BigTextStyle())
                                    .setAutoCancel(true)
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                            notificationManager.notify(1121, builder.build());

                        }
                        else if(SockMngr.response.equals("CRASH"))
                        {
                            stopLocationService();
                            Log.d("ERROR", "SERVER CRASHED");
                        }
                    }
                    catch (Exception e) {
                        Log.d(TAG, "*****exception****");
                        e.printStackTrace();
                    }

                }
                Log.d(TAG, "*****finished function");
            }
        };
    }

    // Called by the system every time a client explicitly starts the service
    // starts the service and returns the value that indicates
    // what semantics the system should use for the service's current started state
    @Override
    public int onStartCommand(Intent intent, int flag, int startId)
    {
        // start location service
        startLocationService();
        //  return the value that indicates what semantics the system should use for the service's current started state
        return START_STICKY;
    }

    // called through stopService() and stops the service
    @Override
    public void onDestroy()
    {
        Log.d(TAG, "*****DESTROY****");
        stopLocationService();
    }

    // stops the service (called by the system)
    @Override
    public boolean stopService(Intent name) {
        Log.d(TAG,"STOP SERVICE");
        stopLocationService();
        return super.stopService(name);
    }

    // called if the service is currently running and the user has removed a task that comes from the service's application.
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG,"ON TASK REMOVED");

        super.onTaskRemoved(rootIntent);
    }

    // starts the service
    @SuppressLint("MissingPermission")
    private void startLocationService()
    {
        /// get notification manager
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(this, MainActivity.class); // will take to the mainActivity when notification is clicked
        // to set it on a notification, must create a pending intent
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), REQUEST_CODE, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT); //Flag indicating that if the described PendingIntent already exists, then keep it but replace its extra data with what is in this new Intent.
        // create and configure the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        configNotification(builder, pendingIntent);


        // notification is necessary only if the version is over 26
        // check where was put it my service
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            // if notification channel is empty
            if(notificationManager != null && notificationManager.getNotificationChannel(CHANNEL_ID) == null)
            {
                // create the notification channel
                NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME


                        , NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.setDescription(CHANNEL_DESCRIPTION);
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        // create and configure location request
         locationRequest = new LocationRequest();
        configLocationRequest();

        // request location updates according to the parameters in the locationRequest - callBacks performed on the mainLooper (in the mainThread)
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, locationCallBack, Looper.getMainLooper());
        // start service!
        startForeground(Constants.LOCATION_SERVICE_ID, builder.build());



    }

    // stop the service
    private void stopLocationService()
    {
        Log.d("location", "*****STOPPED****");
        // Remove all location updates for the given location result listener
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(locationCallBack);
        // stop the service and remove the notification
        stopForeground(true);
        stopSelf();
    }



    // get the user's address (street, town, etc..)
    private String getUserAddress(double latitude, double longitude)
    {
        // get address from location and show it
        Geocoder geocoder = new Geocoder(LocationService.this);
        try
        {
            List<Address> addressList = geocoder.getFromLocation(latitude, longitude, Constants.MAX_RESULTS);
            return (addressList.get(0).getAddressLine(0));

        }
        catch (Exception e)
        {
           return("Unable to get address");
        }
    }

    //  set the various fields of the notification
    public void configNotification(NotificationCompat.Builder builder, PendingIntent pendingIntent)
    {
        builder.setSmallIcon(R.drawable.ic_baseline);
        builder.setContentTitle("Tracking Location");
        builder.setContentText("Tracking your location... You may disable the service at any time");
        builder.setContentIntent(pendingIntent);
        builder.setStyle(new NotificationCompat.BigTextStyle()); // make notification expandable
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
    }

    // configure location request
    public void configLocationRequest()
    {
        locationRequest.setInterval(DEFAULT_INTERVAL); // set interval in which we want to get location in
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    // used only in bound services yet must be overridden
    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    // get username from shared prefrences
    public String get_username()
    {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String username = mPreferences.getString("username", "");
        return username;
    }

}
