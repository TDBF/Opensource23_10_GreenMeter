package com.example.greenmeter;


import com.example.greenmeter.database.LocationData;
import com.example.greenmeter.database.dbCommand;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.jetbrains.annotations.Nullable;

import java.util.Date;


public class  Timeline extends Fragment implements OnMapReadyCallback, LocationListener {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private View view;
    private GoogleMap mMap;
    private LocationManager locationManager;
    private FirebaseAuth mFirebaseAuth; //파이어베이스 인증
    private DatabaseReference mDatabaseRef; //실시간 데이터베이스
    private final int MIN_TIME = 2000;
    private final int MIN_DISTANCE = 5; // 업데이하는 기준이동거리
    private int zm = 14;
    private dbCommand dbcommand;
    private String userID;
    private CO2Calculation co2Calculation;
    private String carName = "BMW_320d";


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout and initialize the map
        view = inflater.inflate(R.layout.timeline, container, false);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // Initialize the FusedLocationProviderClient
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize the LocationManager
        locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("GreenMeter");
        dbcommand = new dbCommand();
        co2Calculation = new CO2Calculation();
        return view;
    }

    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in CBNU
        LatLng myLocation = new LatLng(36.6283933, 127.459223);

        mMap.addMarker(new MarkerOptions()
                .position(myLocation)
                .title("My Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, zm));

    }

    @Override
    public void onResume() {
        super.onResume();
        // Request location updates when the fragment is resumed
        startLocationUpdates();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Stop location updates when the fragment is paused
        stopLocationUpdates();
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                // 권한이 거부되었을 때 처리
            }
        }
    }

    private void startLocationUpdates() {
        // Check for location permission
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Request location updates
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
            // Get the last known location and update the map
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                LatLng latLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                // Update the map with the last known location
                updateMapWithLocation(latLng);
            }
        } else {
            // Location permission not granted, handle accordingly
        }
    }

    private void stopLocationUpdates() {
        // Stop location updates
        locationManager.removeUpdates(this);
    }

    private void updateMapWithLocation(LatLng latLng) {
        String id;
        Date currentDate = new Date();
        String now = currentDate.toString();
        double lat = latLng.latitude;
        double lng = latLng.longitude;


        //이걸로 사용자 Token 가져올 수 있지 않을까...?
        FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
        userID = firebaseUser.getUid();
        id = dbcommand.GetAutoIncrement("GreenMeter/UserLocation/"+userID);
        LocationData location_data = new LocationData();
        location_data.setRecode_time(now);
        location_data.setLat(lat);
        location_data.setLng(lng);
        location_data.setType_trans(carName);

        mDatabaseRef.child("UserLocation").child(userID).child(id).setValue(location_data);

        // Debug 마커표시
        if (mMap != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng).title("현재 위치"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zm));
        }

//        if(Integer.valueOf(id) > ) {
//            co2Calculation.setTimelineData(id, userID);
//        }
    }

    // Implement LocationListener methods
    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        // Update the map with the new location
        updateMapWithLocation(latLng);
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Handle provider enabled event
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Handle provider disabled event
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Handle status changed event
    }

}
