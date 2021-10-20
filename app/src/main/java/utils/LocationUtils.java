package utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static android.content.Context.LOCATION_SERVICE;

@SuppressLint("MissingPermission")
public class LocationUtils {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 123;
    static double lat;
    static double longi;
    static LatLng latLng;

    public static String getLocation(Activity _this) {
        if (ActivityCompat.checkSelfPermission(_this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.checkSelfPermission(_this, Manifest.permission.ACCESS_FINE_LOCATION);
            ActivityCompat.checkSelfPermission(_this, Manifest.permission.ACCESS_COARSE_LOCATION);
            _this.requestPermissions(new String[]{ android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
        }

        LocationManager locationManager = (LocationManager) _this.getSystemService(LOCATION_SERVICE);

        android.location.Location liveLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (liveLocation != null) {
            lat = liveLocation.getLatitude();
            longi = liveLocation.getLongitude();
            latLng = new LatLng(lat,longi);
        } else {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(_this);
            fusedLocationClient.getLastLocation().addOnSuccessListener(_this, location -> {
                if (location != null){
                    lat = location.getLatitude();
                    longi = location.getLongitude();
                    latLng = new LatLng(lat,longi);
                }
            });
        }

        if(latLng==null){
            return "Unknown Location";
        }

        return getAddress(lat, longi, _this);
    }

    public static String getAddress(double lat, double lng, Activity _this) {
        Geocoder geocoder = new Geocoder(_this, Locale.getDefault());
        Address address = null;
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            address = addresses.get(0);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(_this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return address.getCountryName();
    }

}
