package com.example.currentlocation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import static com.example.currentlocation.Constants.EMPTY;
import static com.example.currentlocation.Constants.PERMISSION_PLACE;

public class MainActivity extends AppCompatActivity
{


    private final static int PERMISSION_FINE_LOCATION = 1; //permission request number, identifies the permission
    public ToggleButton toggle; // used to switch the service on and off
    private Dialog dialog; // dialog manager
    private EditText dialogEt; // EditText manager
    private String username; // client's username - identifies the device
    private Button doneBtn, sickbtn, notifyBtn; // Buttons managers
    private SharedPreferences mPreferences; // Interface for accessing and modifying preference data
    private SharedPreferences.Editor mEditor; //All change made in an editor are batched, and not copied back to the original SharedPreferences until commit/ apply
    private CountDownTimer countDownTimer ; // timer that checks is the service running and determines in the server up
    public boolean serviceControl = false ; // determines has the service been shut down by the user
    public boolean onStart = true;

    // sets the initial state when the app is opened
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // get username if exists
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mPreferences.edit();
        if(mPreferences.getString("username", "").equals(""))
        {
            createAndHandleDialog();
        }
        this.username = mPreferences.getString("username", "");

        this.countDownTimer = null;


        sickbtn = findViewById(R.id.sickbtn);
        sickbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAndHandleSickDialog();
            }
        });
        // create toggle button
        toggle = (ToggleButton) findViewById(R.id.toggleButton);
        // present the toggle button as on if the app has been opened again and the service is still running
        if(isLocationServiceRunning())
        {
            toggle.setChecked(true);
        }
        // set a listener for interactions
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            // handle toggle button
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                // when clicked - start or stop the service according to the users choice
                if (isChecked)
                {
                    toggle.setChecked(false);
                   // if the app didn't get the required permissions
                    if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    {
                            // request fine location permission
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);

                    }
                    else
                    {
                        toggle.setChecked(true);
                        try {
                            // initiate socket
                            SockMngr.initiate();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (SockMngr.response.equals("FAILURE"))
                        {
                            Toast.makeText(MainActivity.this, "Couldn't connect to the server", Toast.LENGTH_LONG).show();
                            toggle.setChecked(false);
                        }
                        else
                            {
                                serviceControl = false;
                            // send username and status - sync
                            String status = mPreferences.getString("status", "");
                            // should it be in try catch? **
                            SockMngr.sendAndReceive(username + "," + status);
                            Log.d("MAIN ACTIVITY", "SYNCED");
                            // the app has permission - start location service
                            startLocationService();
                        }
                    }
                    startTimer();
                }
                else
                {
                    serviceControl = true;
                    cancelTimer();
                    Log.d("Location Update", "Button clicked");
                    // the button has been switched off - stop location service
                    stopLocationService();
                    // disconnect from server
                    Log.d("******", "Sending quit!!");
                    SockMngr.sendAndReceive(username + "," + "QUIT");
                    Log.d("******", "Sent quit!!");
                }
            }
        });
    }

    // creates and handles the dialog box for declaring sickness
    private void createAndHandleSickDialog()
    {
        // create dialog
        dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.sick_dialog);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.background));
        }
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);
        dialog.getWindow().getAttributes().windowAnimations = R.style.animation;
        dialog.show();

        // initiate done button
        notifyBtn = dialog.findViewById(R.id.notifyBtn);
        notifyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                try {
                    if (!isLocationServiceRunning()) {
                        // initiate socket
                        SockMngr.initiate();
                    }
                    Log.d("Main activity", "after initiate");
                    if (SockMngr.response.equals("FAILURE"))
                    {
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, "Couldn't connect to the server", Toast.LENGTH_LONG).show();
                    }
                    else
                        {
                        // declare
                        SockMngr.sendAndReceive(username + "," + "SICK");
                        if (!isLocationServiceRunning()) {
                            // disconnect from server
                            SockMngr.sendAndReceive(username + "," + "QUIT");
                        }
                        dialog.dismiss();
                        mEditor.putString("status", "sick");
                        mEditor.commit();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    // Callback for the result from requesting permissions.
    @Override
    public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        // if permission is granted
        if(requestCode == PERMISSION_FINE_LOCATION && grantResults.length>EMPTY)
        {
            if(grantResults[PERMISSION_PLACE] == PackageManager.PERMISSION_GRANTED)
            {
                toggle.setChecked(true);
                try {
                    // initiate socket
                    SockMngr.initiate();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                if (SockMngr.response.equals("FAILURE"))
                {
                    Toast.makeText(MainActivity.this, "Couldn't connect to the server", Toast.LENGTH_LONG).show();
                    toggle.setChecked(false);
                }
                else
                {
                    serviceControl = false;
                    Log.d("***", "after do initiate, going to start");
                    // send username and status - sync
                    String status = mPreferences.getString("status", "");
                    // should it be in try catch? **
                    SockMngr.sendAndReceive(username + "," + status);
                    Log.d("MAIN ACTIVITY", "SYNCED");
                    // the app has permission - start location service
                    startLocationService();
                }
            }
            else
            {
                // didn't get permission, notify user
                Toast.makeText(this, "You must enable these permissions in order to to use this app", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // returns is the location service running (as foreground)
    private boolean isLocationServiceRunning()
    {
        // get to the handle to the ActivityManager for interacting with the global activity state of the system.
        ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        if(activityManager != null)
        {
            // goes over every service in the system that is currently running
            // get running services - returns a list of RunningServiceInfo records describing each of the running tasks.
            // the max_value represents the maximum number of entries to return in the list
            for(ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE))
            {
                // if the running service being inspected is the location service
                if(LocationService.class.getName().equals(service.service.getClassName()))
                {
                    // check if its running as foreground
                    if(service.foreground)
                    {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    // triggers starting the location service
    private void startLocationService()
    {
        // if location service isn't running
        if(!isLocationServiceRunning())
        {
            // create an intent to trigger the service
            Intent intent = new Intent(getApplicationContext(), LocationService.class); // second parameter is the class we want to open
            // start the service
            ContextCompat.startForegroundService(this, intent);
            Toast.makeText(this, "Location Service has started",Toast.LENGTH_SHORT).show();
        }

    }

    // triggers stopping the location service
    private void stopLocationService()
    {
        // if location service is running
        if(isLocationServiceRunning())
        {
            // create an intent to trigger the service
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            // stop location service
            stopService(intent);
            Toast.makeText(this, "Location Service stopped", Toast.LENGTH_SHORT).show();
        }
    }

    // creates and handles a dialog window for signing up
    private void createAndHandleDialog()
    {
        // create dialog
        dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.custom_dialog);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.background));
        }
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(false);
        dialog.getWindow().getAttributes().windowAnimations = R.style.animation;
        dialog.show();
        TextView errorTv = dialog.findViewById(R.id.errorTv);

        // initiate done button
        doneBtn = dialog.findViewById(R.id.doneBtn);
        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                errorTv.setVisibility(View.INVISIBLE);
                // ** add wait **
                dialogEt = dialog.findViewById(R.id.dialogEt);
                username = dialogEt.getText().toString();
                handle_username(username, errorTv);

            }
        });
        this.username = mPreferences.getString("username", "");
        if(this.username!=null)
        {
            Log.d("username is", this.username);
        }
        else
        {
            Log.d("username is ", "null :) ");
        }
    }

    // addresses the server, if the username is free sets it, if not shows an appropriate message
    private void handle_username(String username, TextView errorTv)
    {
        // connect to server
        try {
            // initiate socket
            SockMngr.initiate();
            if(SockMngr.response.equals("FAILURE"))
            {
                Log.d("Main Activity", "Username failure");
                Toast.makeText(this, "Couldn't connect to the server", Toast.LENGTH_SHORT).show();
                return;
            }
            // send username
            SockMngr.sendAndReceive(username);
            if(SockMngr.response.equals("CRASH"))
            {
                Log.d("Main Activity", "Username failure");
                Toast.makeText(this, "Couldn't connect to the server", Toast.LENGTH_SHORT).show();
                return;
            }
            if(SockMngr.response.equals("FREE"))
            {
                mEditor.putString("username", username);
                mEditor.putString("status", "healthy");
                mEditor.commit();
                dialog.dismiss();
            }
            else if (SockMngr.response.equals("OCCUPIED"))
            {
                // otherwise present an error
                errorTv.setVisibility(View.VISIBLE);
            }
            else
            {
                Log.d("Main activity", "Unexpected error");
                Toast.makeText(this, "Please try again later", Toast.LENGTH_SHORT).show();
                return;
            }
            // disconnect from server
            SockMngr.sendAndReceive(username + "," + "QUIT");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    //start timer function
    public void startTimer()
    {
        this.countDownTimer = new CountDownTimer(30000, 1000)
        {
            public void onTick(long millisUntilFinished)
            {
                if(isLocationServiceRunning())
                {
                    onStart = false;
                }
                else if(!onStart && !serviceControl)
                {
                    Log.d("MAIN ACTIVITY", "SERVICE ISN'T RUNNING");
                    toggle.setChecked(false);
                    Toast.makeText(MainActivity.this, "Connection failure, please try again later", Toast.LENGTH_SHORT).show();
                    this.cancel();
                    Log.d("MAIN ACTIVITY", "im here");
                }
            }
            public void onFinish()
            {
                Log.d("Main activity", "timer finished");
                startTimer();
            }
        };
        this.countDownTimer.start();
    }

    //cancel timer
    public void cancelTimer() {
        if(this.countDownTimer!=null)
        {
            this.countDownTimer.cancel();
            onStart = true;
        }

    }
}