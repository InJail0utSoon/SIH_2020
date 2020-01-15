package com.ait.mainactivity;

/* ******************************************HEADERS********************************************** */

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;

import android.os.Looper;
import android.os.Bundle;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.location.FusedLocationProviderClient;

import android.location.Location;
import android.view.View;

import java.util.concurrent.TimeUnit;


/* ******************************************MapsActivity********************************************** */

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    // map reference
    private GoogleMap mMap;

    // cam position describer
    private CameraPosition camPos;

    // location utility tools
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mRecentLocation;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    // onCreate method
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // getting permissions and initializing location gathering tools
        init();

    }

    // initializing location gathering utilities
    private void init()
    {
        findViewById(R.id.AroundMe).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                localize(v);

            }
        });

        locationRequest = new LocationRequest()
                .setInterval(TimeUnit.SECONDS.toMillis(10))
                .setFastestInterval(TimeUnit.SECONDS.toMillis(10))
                .setExpirationDuration(TimeUnit.SECONDS.toMillis(Long.MAX_VALUE))
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        //dummy value for location
        mRecentLocation = new Location("dummy provider");
        mRecentLocation.setLatitude(12);
        mRecentLocation.setLatitude(12);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {

                if (locationResult != null)
                {
                    for (Location location : locationResult.getLocations()) {

                        mRecentLocation = location;

                        System.out.println("@ "+location.getLongitude()+" "+location.getLatitude());

                    }
                }

            }

        };


        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

    }

    // moving cam view to users location
    public void localize(View view)
    {

        camPos = new CameraPosition.Builder()
                .target(new LatLng(mRecentLocation.getLatitude(),mRecentLocation.getLongitude()))
                .zoom(15)
                .bearing(45)
                .tilt(70)
                .build();

        CameraUpdate camUpd = CameraUpdateFactory.newCameraPosition(camPos);
        mMap.animateCamera(camUpd);

    }


    // initializing the map once its loaded
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(28,80), 60));
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

    }


    @Override
    protected void onResume() {
        super.onResume();

        startLocationUpdates();

    }

    // starts location gathering loop
    private void startLocationUpdates() {

        mFusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }


    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    // stops location gathering loop
    private void stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }


}
