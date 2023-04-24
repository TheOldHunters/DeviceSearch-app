package com.de.search.util.maps;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;


import androidx.core.app.ActivityCompat;

import org.bingmaps.sdk.Coordinate;

public class GPSManager {
    private LocationManager _locationManager;
    private String _bestProvider;
    private Activity _activity;
    private LocationListener _listener;

    //Receives an Activity and a LocationListener object as parameters. It initialises a LocationManager object and a _bestProvider variable.
    public GPSManager(Activity activity, LocationListener listener) {
        _activity = activity;
        _listener = listener;
        _locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        if (_bestProvider == null) {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            _bestProvider = _locationManager.getBestProvider(criteria, false);
        }

        if (_bestProvider != null) {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                _locationManager.requestLocationUpdates(_bestProvider, Constants.GPSTimeDelta, Constants.GPSDistanceDelta, listener);
            }
        }
    }

    //Gets the last known position from the device's GPS and returns its coordinates as a Coordinate object. If the position cannot be obtained, null is returned.
    public Coordinate GetCoordinate() {
        if (_bestProvider != null) {
            if (ActivityCompat.checkSelfPermission(_activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(_activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location location = _locationManager.getLastKnownLocation(_bestProvider);
                if (location != null) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    return new Coordinate(lat, lon);
                }
            }
        }
        return null;
    }

    public void refresh() {
        if (_bestProvider == null) {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            _bestProvider = _locationManager.getBestProvider(criteria, false);
        }
        if (_bestProvider != null) {
            if (ActivityCompat.checkSelfPermission(_activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(_activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                _locationManager.requestLocationUpdates(_bestProvider, Constants.GPSTimeDelta, Constants.GPSDistanceDelta, _listener);
            }
        }
    }
}
