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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.location.Location;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Response;
import okhttp3.ResponseBody;


/* ******************************************MapsActivity********************************************** */

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    // map reference
    private GoogleMap mMap;

    private LinkedList<Marker> potholes;
    // cam position describer
    private CameraPosition camPos;

    // location utility tools
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mRecentLocation;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    // onCreate method
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        System.out.println("@MapsActivity.onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // getting permissions and initializing location gathering tools
        init();

    }

    /********************  Initialization function  *****************/

    // initializing location gathering utilities
    private void init()
    {
        System.out.println("@MapsActivity.init");

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

                        System.out.println("@distance :: "+distance(mRecentLocation,location));

                        if(distance(mRecentLocation,location) > 2.5)
                        {


                            refreshData(location);

                        }

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


        potholes = new LinkedList<Marker>();

    }


    private double distance(Location l1,Location l2)
    {
        Double lon1 = l1.getLongitude();
        Double lon2 = l2.getLongitude();

        Double lat1 = l1.getLatitude();
        Double lat2 = l2.getLatitude();

        if ((lat1 == lat2) && (lon1 == lon2))return 0;

        else
        {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;

            dist = dist * 1.609344;

            return (dist);
        }
    }

    /********************  Map Marker Refresh  *****************/

    private void refreshData(Location location)
    {

        System.out.println("@MapsActivity.refreshData");

        DatabaseManager.getInstance().getNearbyData(location.getLongitude(),location.getLatitude(),new Callback(){

            @Override
            public void onFailure(Call call, IOException e)
            {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {

//                Marker m = mMap.addMarker(new MarkerOptions()
//                        .position(
//                                new LatLng(73.8747327,18.606458))
//                        .draggable(false).visible(true));

                System.out.println("@onResponse :: "+response.body().string());

                try {

                    for(Marker marker:potholes)
                    {
                        marker.remove();
                    }

                    potholes.clear();

                    System.out.println("____________________________");

                    JSONArray jsonArray = new JSONArray(response.body().string());

                    System.out.println("@Response :: "+jsonArray.length());

                    for(int i = 0; i < jsonArray.length();i++)
                    {

                        JSONObject jsonObject = jsonArray.getJSONObject(i);

                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(
                                        new LatLng(Double.valueOf(jsonObject.getString("latitude")),
                                                Double.valueOf(jsonObject.getString("longitude"))))
                                .draggable(false).visible(true));

                        potholes.add(marker);

                    }

                }

                catch(Exception ex)
                {

                    ex.printStackTrace();
                }
            }

        });

    }

    /********************  Localize Map View  *****************/

    // moving cam view to users location
    public void localize(View view)
    {

        System.out.println("@MapsActivity.localize");

        camPos = new CameraPosition.Builder()
                .target(new LatLng(mRecentLocation.getLatitude(),mRecentLocation.getLongitude()))
                .zoom(15)
                .bearing(45)
                .tilt(70)
                .build();

        CameraUpdate camUpd = CameraUpdateFactory.newCameraPosition(camPos);
        mMap.animateCamera(camUpd);

    }

    /********************  Initial Map Construction  *****************/

    // initializing the map once its loaded
    @Override
    public void onMapReady(GoogleMap googleMap) {

        System.out.println("@MapsActivity.onMapReady");

        mMap = googleMap;

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(28,80), 60));
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

    }


    @Override
    protected void onResume() {

        System.out.println("@MapsActivity.onResume");

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
