package com.gotobus;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class Passenger {
    String bookingId, phone, name, otp, fare, age, gender, status;
    LatLng pickup_point, dropoff_point;
    MarkerOptions pickupMarkerOptions, dropoffMarkerOptions;
    Marker pickupMarker, dropoffMarker;

    public Passenger(String bookingId, String phone, String name, String pickup_point, String dropoff_point, String otp, String fare, String gender, String age, String status) {
        this.bookingId = bookingId;
        this.phone = phone;
        this.name = name;
        this.gender = gender;
        this.age = age;
        String[] pp = pickup_point.split(",");
        String[] dp = dropoff_point.split(",");
        this.pickup_point = new LatLng(Double.parseDouble(pp[0]), Double.parseDouble(pp[1]));
        this.dropoff_point = new LatLng(Double.parseDouble(dp[0]), Double.parseDouble(dp[1]));
        this.otp = otp;
        this.fare = fare;
        this.status = status;

        if (status.equals("PICKED")) {
            pickupMarkerOptions = new MarkerOptions()
                    .position(this.pickup_point)
                    .title(name)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.pickedup_marker));
        } else {
            pickupMarkerOptions = new MarkerOptions()
                    .position(this.pickup_point)
                    .title(name)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.pickup_marker));
        }

        dropoffMarkerOptions = new MarkerOptions()
                .position(this.dropoff_point)
                .title(name)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.dropoff_marker));
    }
}