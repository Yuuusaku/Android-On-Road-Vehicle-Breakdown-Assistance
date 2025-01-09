package com.example.helpimstuck;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class Map_Fragment extends Fragment implements OnMapReadyCallback {

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private boolean isLocationUpdateRequested = false;
    private GoogleMap googleMap;
    private Marker currentLocationMarker;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map_, container, false);

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.MY_MAP);
        supportMapFragment.getMapAsync(this);

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // Enable user's location on the map
        enableMyLocation();
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireActivity(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            requestLocationUpdates();
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }
    }

    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        if (!isLocationUpdateRequested) {
            isLocationUpdateRequested = true;

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }

                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        updateMarkerPosition(location.getLatitude(), location.getLongitude());
                    }
                }
            };

            // Request location updates
            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setInterval(10000); // The location Updates every 10 seconds
            locationRequest.setFastestInterval(5000); // The fastest update interval in milliseconds
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void updateMarkerPosition(double latitude, double longitude) {
        LatLng currentLatLng = new LatLng(latitude, longitude);

        if (currentLocationMarker == null) {
            // Create a new marker if it doesn't exist
            currentLocationMarker = googleMap.addMarker(new MarkerOptions().position(currentLatLng).title("Current Location"));
        } else {
            // Move the existing marker to the updated location
            currentLocationMarker.setPosition(currentLatLng);
        }

        // Move the camera to the updated location only if it's the first update
        if (googleMap.getCameraPosition().target.latitude == 0 && googleMap.getCameraPosition().target.longitude == 0) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop location updates when the view is destroyed
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isLocationUpdateRequested = false;
        }
    }

    // Add the following method
    public Location getCurrentLocation() {
        Location lastLocation = null;
        if (ActivityCompat.checkSelfPermission(requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireActivity(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            lastLocation = fusedLocationClient.getLastLocation().getResult();
        }
        return lastLocation;
    }
}
