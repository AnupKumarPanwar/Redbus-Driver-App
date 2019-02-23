package com.gotobus;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class Passenger {
    String phone, name, otp, fare;
    LatLng pickup_point, dropoff_point;
    MarkerOptions pickupMarkerOptions, dropoffMarkerOptions;
    Marker pickupMarker, dropoffMarker;

    public Passenger(String phone, String name, String pickup_point, String dropoff_point, String otp, String fare) {
        this.phone = phone;
        this.name = name;
        String[] pp = pickup_point.split(",");
        String[] dp = dropoff_point.split(",");
        this.pickup_point = new LatLng(Double.parseDouble(pp[0]), Double.parseDouble(pp[1]));
        this.dropoff_point = new LatLng(Double.parseDouble(dp[0]), Double.parseDouble(dp[1]));
        this.otp = otp;
        this.fare = fare;
        pickupMarkerOptions =new MarkerOptions()
                .position(this.pickup_point)
                .title(name)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.pickup_marker));

        dropoffMarkerOptions =new MarkerOptions()
                .position(this.dropoff_point)
                .title(name)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.dropoff_marker));
    }
}