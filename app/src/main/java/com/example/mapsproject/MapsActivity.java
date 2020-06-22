package com.example.mapsproject;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements
        GoogleMap.OnCameraMoveStartedListener,
        GoogleMap.OnCameraMoveListener,
        GoogleMap.OnCameraMoveCanceledListener,
        GoogleMap.OnCameraIdleListener,
        OnMapReadyCallback {


    private GoogleMap mMap;
    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    private ResultReceiver resultReceiver;
    Button buttonLocation;
    TextView textLat;
    TextView textAddy;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        buttonLocation = findViewById(R.id.buttonLocation);
        textLat = findViewById(R.id.textLatitude);
        textAddy = findViewById(R.id.textAddress);
        resultReceiver = new AddressResultReceiver(new Handler());
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
//        fetchLastLocation();

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.setOnCameraIdleListener(MapsActivity.this);
        mMap.setOnCameraMoveStartedListener(MapsActivity.this);
        mMap.setOnCameraMoveListener(MapsActivity.this);
        mMap.setOnCameraMoveCanceledListener(MapsActivity.this);

        // Show Sydney on the map.
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-33.87365, 151.20689), 10));
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        } else {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setIndoorLevelPickerEnabled(true);
            createLocationRequest();
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
    }

    protected void createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.getFusedLocationProviderClient(MapsActivity.this).requestLocationUpdates(locationRequest, new LocationCallback() {
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                LocationServices.getFusedLocationProviderClient(MapsActivity.this).removeLocationUpdates(this);
                if (locationResult != null && locationResult.getLocations().size() > 0) {
                    int latestLocationIndex = locationResult.getLocations().size() - 1;
                    double latitude = locationResult.getLocations().get(latestLocationIndex).getLatitude();
                    double longitude = locationResult.getLocations().get(latestLocationIndex).getLongitude();
                    textLat.setText(String.format("Latitude: %s\nLongitude: %s", latitude, longitude));
                    Location location = new Location("providerNA");
                    location.setLatitude(latitude);
                    location.setLongitude(longitude);
                    LatLng Location = new LatLng(latitude, longitude);
                    mMap.addMarker(new MarkerOptions().position(Location));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(Location));
                    fetchAddressFromLatLong(location);
                }

            }
        }, Looper.getMainLooper());
    }

    @Override
    public void onCameraMove() {
        Toast.makeText(this, "The camera is moving.",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraMoveCanceled() {
        Toast.makeText(this, "Camera movement canceled.",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraIdle() {
        Toast.makeText(this, "The camera has stopped moving.",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraMoveStarted(int i) {

    }

    private class AddressResultReceiver extends ResultReceiver {
        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            if (resultCode == Constants.SUCCESS_RESULT)
                textAddy.setText(resultData.getString(Constants.RESULT_DATA_KEY));
            else
                Toast.makeText(MapsActivity.this, resultData.getString(Constants.RESULT_DATA_KEY), Toast.LENGTH_SHORT).show();
        }

    }

    private void fetchAddressFromLatLong(Location location) {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, resultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, location);
        startService(intent);
    }
}
