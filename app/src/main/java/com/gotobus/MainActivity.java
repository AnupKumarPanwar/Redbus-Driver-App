package com.gotobus;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private GoogleMap mMap;

    private final LatLng mDefaultLocation = new LatLng(28.7041, 77.1025);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    private Location mLastKnownLocation;

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private CameraPosition mCameraPosition;
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    MarkerOptions sourceMarkerOption, destinationMarkerOption;

    int AUTOCOMPLETE_SOURCE = 1, AUTOCOMPLETE_DESTINATION = 2;

    EditText sourceAddress, destinationAddress;

    LinearLayout container, nextPassengerContainer;

    boolean tripStarted = false;
    FloatingActionButton fab;

    int routeId = -1;
    String baseUrl;
    SharedPreferences sharedPreferences;
    String PREFS_NAME = "MyApp_Settings";
    String accessToken;
    SharedPreferences.Editor editor;

    Handler handler;
    Runnable updateBusLocationRunnable;

    ArrayList<Passenger> passengers;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        //toolbar.setNavigationIcon(R.drawable.ic_toolbar);
        toolbar.setTitle("");
        toolbar.setSubtitle("");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        baseUrl = getResources().getString(R.string.base_url);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        accessToken = sharedPreferences.getString("access_token", null);

        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        passengers = new ArrayList<>();

        Intent intent = new Intent(this, UpdateLocationReceiver.class);
        Bundle locationBundle = new Bundle();
        locationBundle.putString("baseUrl", baseUrl);
        locationBundle.putString("Authorization", accessToken);
        intent.putExtras(locationBundle);
        alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);


        AndroidNetworking.initialize(getApplicationContext());

        container = findViewById(R.id.container);
        container.requestFocus();

        nextPassengerContainer = findViewById(R.id.next_passenger_container);

        sourceAddress = findViewById(R.id.source_address);
        destinationAddress = findViewById(R.id.destination_address);

        handler = new Handler();
        updateBusLocationRunnable = new Runnable() {
            @Override
            public void run() {
//                updateBusLocation();
                getBookings();
                if (tripStarted) {
                    handler.postDelayed(this, 12000);
                }
            }
        };


        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!tripStarted) {
                    startTrip();
                } else {
                    endTrip();
                }
            }
        });


        checkActiveTrip();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        toolbar.setTitleTextColor(Color.BLACK);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void getBookings() {
        AndroidNetworking.post(baseUrl + "/getBookings.php")
                .setOkHttpClient(NetworkCookies.okHttpClient)
                .addHeaders("Authorization", accessToken)
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject result = response.getJSONObject("result");
                            boolean success = Boolean.parseBoolean(result.get("success").toString());
                            if (success) {
                                JSONArray data = result.getJSONArray("data");
                                for (Passenger passenger : passengers) {
                                    passenger.pickupMarker.remove();
                                    passenger.dropoffMarker.remove();
                                }
                                passengers.clear();
                                for (int i = 0; i < data.length(); i++) {
                                    String phone = data.getJSONObject(i).get("phone").toString();
                                    String name = data.getJSONObject(i).get("name").toString();
                                    String pickup_point = data.getJSONObject(i).get("pickup_point").toString();
                                    String dropoff_point = data.getJSONObject(i).get("dropoff_point").toString();
                                    String otp = data.getJSONObject(i).get("otp").toString();
                                    String fare = data.getJSONObject(i).get("fare").toString();

                                    passengers.add(new Passenger(phone, name, pickup_point, dropoff_point, otp, fare));
                                }
//                                Toast.makeText(getApplicationContext(), passengers.toString(), Toast.LENGTH_LONG).show();
                                for (Passenger passenger : passengers) {
                                    passenger.pickupMarker = mMap.addMarker(passenger.pickupMarkerOptions);
                                    passenger.dropoffMarker = mMap.addMarker(passenger.dropoffMarkerOptions);
                                }
                            }
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();

                        }
                    }

                    @Override
                    public void onError(ANError anError) {

                    }
                });
    }

    public void startTrip() {
        Snackbar.make(getCurrentFocus(), "Trip started", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
        fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_stop_black_24dp));
        nextPassengerContainer.setVisibility(View.VISIBLE);
        tripStarted = true;
        alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                60 * 1000 * 2, alarmIntent);
        handler.postDelayed(updateBusLocationRunnable, 100);
    }

    public void endTrip() {
        Snackbar.make(getCurrentFocus(), "Trip completed", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
        fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_black_24dp));
        nextPassengerContainer.setVisibility(View.GONE);
        tripStarted = false;
        alarmMgr.cancel(alarmIntent);
        handler.removeCallbacksAndMessages(null);
    }

    private void checkActiveTrip() {
        AndroidNetworking.post(baseUrl + "/getActiveTrip.php")
                .setOkHttpClient(NetworkCookies.okHttpClient)
                .addHeaders("Authorization", accessToken)
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject result = response.getJSONObject("result");
                            boolean success = Boolean.parseBoolean(result.get("success").toString());
                            if (success) {
                                int routeId = Integer.parseInt(result.getJSONObject("data").get("route_id").toString());
                                buildRoute(routeId);
                                sourceAddress.setEnabled(false);
                                destinationAddress.setEnabled(false);
                                startTrip();
                            } else {
                                sourceAddress.setEnabled(true);
                                destinationAddress.setEnabled(true);
                                endTrip();
                            }
                        } catch (Exception e) {

                        }
                    }

                    @Override
                    public void onError(ANError anError) {

                    }
                });
    }

    private void updateBusLocation() {

        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();

                        } else {
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }


        AndroidNetworking.post(baseUrl + "/updateLatLong.php")
                .setOkHttpClient(NetworkCookies.okHttpClient)
                .addHeaders("Authorization", accessToken)
                .addBodyParameter("last_location", String.valueOf(mLastKnownLocation.getLatitude()) + "," + String.valueOf(mLastKnownLocation.getLongitude()))
                .addBodyParameter("bearing", String.valueOf(mLastKnownLocation.getBearing()))
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {

                    @Override
                    public void onResponse(JSONObject response) {
//                        Toast.makeText(getApplicationContext(), response.toString(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(ANError anError) {
//                        Toast.makeText(getApplicationContext(), anError.toString(), Toast.LENGTH_LONG).show();
                    }
                });
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.routes) {
            Intent intent = new Intent(getApplicationContext(), RoutesActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
//        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.style_json));

        mMap.setBuildingsEnabled(true);

        // Prompt the user for permission.
        getLocationPermission();
        updateLocationUI();
        getDeviceLocation();

        if (getIntent().getExtras() != null) {
            routeId = getIntent().getExtras().getInt("route_id", -1);
            if (routeId != -1) {
                buildRoute(routeId);
            }
        }
//        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    public void buildRoute(int routeId) {
        AndroidNetworking.post(baseUrl + "/getRoute.php")
                .setOkHttpClient(NetworkCookies.okHttpClient)
                .addHeaders("Authorization", accessToken)
                .addBodyParameter("route_id", String.valueOf(routeId))
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject result = response.getJSONObject("result");
                            boolean success = Boolean.parseBoolean(result.get("success").toString());
                            if (success) {
                                JSONObject data = result.getJSONObject("data");
                                sourceAddress.setText(data.get("source").toString());
                                destinationAddress.setText(data.get("destination").toString());

                                String[] sourceLatLong = data.get("sourceLatLong").toString().split(",");
                                String[] destinationLatLong = data.get("destinationLatLong").toString().split(",");
                                LatLng origin = new LatLng(Double.parseDouble(sourceLatLong[0]), Double.parseDouble(sourceLatLong[1]));
                                LatLng destination = new LatLng(Double.parseDouble(destinationLatLong[0]), Double.parseDouble(destinationLatLong[1]));
                                String waypoints = data.get("waypoints").toString();

                                sourceMarkerOption = new MarkerOptions()
                                        .position(origin)
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.source_pin));
                                mMap.addMarker(sourceMarkerOption);

                                destinationMarkerOption = new MarkerOptions()
                                        .position(destination)
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destination_pin));
                                mMap.addMarker(destinationMarkerOption);

                                String url = getDirectionsUrl(origin, destination, waypoints);

                                DownloadTask downloadTask = new DownloadTask();

                                // Start downloading json data from Google Directions API
                                downloadTask.execute(url);
                            } else {
                                String message = result.get("message").toString();
                                if (message.equals("Invalid access token.")) {
                                    editor.putString("access_token", null);
                                    editor.commit();
                                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Log.d("onResponse", e.getMessage());
                        }
                    }


                    @Override
                    public void onError(ANError anError) {

                    }
                });
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            try {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            } catch (Exception e) {
                Log.d("getLocationPermission", e.getMessage());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);

//                mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
//                    @Override
//                    public boolean onMyLocationButtonClick() {
//                        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
//                        try{
//                            List<Address> addresses = geocoder.getFromLocation(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude(), 1);
//                            Address address = addresses.get(0);
//                            String currentAddress = address.getAddressLine(0);
//                            sourceAddress.setText(currentAddress);
//                        }
//                        catch (Exception e) {
//                            Log.e("Exception", e.getMessage());
//                        }
//
//                        return false;
//                    }
//                });

            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();

//                            sourceMarkerOption = new MarkerOptions()
//                                    .position(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()))
//                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.source_pin));
//                            mMap.addMarker(sourceMarkerOption);

                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation
                                                    .getLongitude()), DEFAULT_ZOOM));
//                            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                            try {
//                                List<Address> addresses = geocoder.getFromLocation(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude(), 1);
//                                Address address = addresses.get(0);
//                                String currentAddress = address.getAddressLine(0);
//                                sourceAddress.setText(currentAddress);
                            } catch (Exception e) {
                                Log.e("Exception", e.getMessage());
                            }
                        } else {
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();


            parserTask.execute(result);

        }
    }


    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(18);
                lineOptions.color(Color.parseColor("#0fa4e6"));
                lineOptions.geodesic(true);

            }

// Drawing polyline in the Google Map for the i-th route
            try {
                mMap.addPolyline(lineOptions);
            } catch (Exception e) {
                Log.d("Polyline", e.getMessage());
            }
        }
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest, String waypoints) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";
        String mode = "mode=driving";

        String apiKey = "key=" + getResources().getString(R.string.google_maps_key);
        String callback = "callback=initialize";
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode + "&" + apiKey + "&" + callback;

        parameters += "&waypoints=" + waypoints;
        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
}
