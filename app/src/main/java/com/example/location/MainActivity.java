package com.example.location;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends AppCompatActivity {
    FusedLocationProviderClient locationProvider;
    LocationRequest locationRequest;
    LocationCallback callback;
    TextView latText, lonText;
    LatLng coord;
    Switch sw;
    String phoneNo;
    String message;
    PendingIntent sent, delivered;
    BroadcastReceiver sentSMS, deliveredSMS;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latText = (TextView)findViewById(R.id.lat);
        lonText = (TextView) findViewById(R.id.lon);
        sw = (Switch) findViewById(R.id.sw);
        Button map = (Button) findViewById(R.id.mapBtn);
        Button sms = (Button) findViewById(R.id.sms);
        locationRequest = new LocationRequest();
        locationRequest.setInterval(20000);
        locationRequest.setFastestInterval(40000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);

        // set create broadcast intents
        sent = PendingIntent.getBroadcast(MainActivity.this,0, new Intent("SMS_SENT"),0);
        delivered = PendingIntent.getBroadcast(MainActivity.this,0,new Intent("SMS_DELIVERED"),0);


        sms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSMSMessage();
            }
        });
        map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent in = new Intent(MainActivity.this, MapsActivity.class);
                in.putExtra("long",coord.longitude);
                in.putExtra("lat",coord.latitude);
                startActivity(in);
            }
        });
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    openTracker();
                    sw.setText("ON");
                }else{
                    closeTracker();
                    sw.setText("OFF");
                }
            }
        });
        updateGPS();
    }



    protected void sendSMSMessage() {
        phoneNo = "+250789421906";
        message = "My location: "+coord;

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.SEND_SMS)) {


            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        12);
            }
        }else{
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, message, sent, delivered);

        }
    }

    // on pause; the listener are un registared
    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(sentSMS);
        unregisterReceiver(deliveredSMS);
    }

    // on resume (activity is active) the broadcasts are initialized and registered.
    @Override
    protected void onResume(){
        super.onResume();
        sentSMS = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()){
                    case Activity.RESULT_OK:
                        Toast.makeText(context, "SMS Sent !",Toast.LENGTH_LONG).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(context, "No service",Toast.LENGTH_LONG).show();
                        break;
                    default:
                        Toast.makeText(context, "SMS Not Sent !",Toast.LENGTH_LONG).show();

                }
            }
        };

        deliveredSMS = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()){
                    case Activity.RESULT_OK:
                        Toast.makeText(context, "SMS Received!",Toast.LENGTH_LONG).show();

                        break;
                    default:
                        Toast.makeText(context, "SMS Not Received !",Toast.LENGTH_LONG).show();


                }
            }
        };

        registerReceiver(sentSMS, new IntentFilter("SMS_SENT"));
        registerReceiver(deliveredSMS, new IntentFilter("SMS_DELIVERED"));

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case  11:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    updateGPS();
                }else{
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    finish();
                }
            case 12:
                if ( grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(phoneNo, null, message, null, null);
                    Toast.makeText(getApplicationContext(), "SMS sent.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "SMS faild, please try again.", Toast.LENGTH_LONG).show();
                    return;
                }
        }
    }

    // close the activity
    public void closeTracker(){

            locationProvider.removeLocationUpdates(callback);
            lonText.setText("Tracker off");
            latText.setText("Tracker off");

    }

    // open the tracker
    public void openTracker(){
        updateGPS();
    }

    public void updateGPS(){
        locationProvider = LocationServices.getFusedLocationProviderClient(this);
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED)
        {

            callback = new LocationCallback(){
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    coord = new LatLng(locationResult.getLastLocation().getLatitude(),locationResult.getLastLocation().getLongitude());
                    updateUI(locationResult.getLastLocation());
                }
            };


            locationProvider.requestLocationUpdates(locationRequest, callback, null);

        }else{
            if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.M){
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION},11);
            }
        }

    }

    // change the text fields on the UI
    public void updateUI(Location loc){
        latText.setText(loc.getLatitude()+"");
        lonText.setText(loc.getLongitude()+"");
    }

}