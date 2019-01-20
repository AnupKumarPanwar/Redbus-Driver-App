package com.gotobus;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.woxthebox.draglistview.DragItemAdapter;
import com.woxthebox.draglistview.DragListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AddRouteActivity extends AppCompatActivity {

    ArrayList<RouteWaypoint> mItemArray;
    int AUTOCOMPLETE_WAYPOINT = 1;
    long i=0;
    ItemAdapter listAdapter;
    int routeId = -1;
    TextView activityTitle;
    String baseUrl;
    SharedPreferences sharedPreferences;
    String PREFS_NAME = "MyApp_Settings";
    String accessToken;
    TimePicker timePicker;

    String source;
    String destination;
    String sourceLatLong;
    String destinationLatLong;
    String departure_time;

    String waypoints = "";
    String waypointsLatLong = "";
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_route);

        baseUrl = getResources().getString(R.string.base_url);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        accessToken = sharedPreferences.getString("access_token", null);

        progressDialog=new ProgressDialog(this);
        progressDialog.setMessage("Fetching routes...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(true);
        progressDialog.setProgress(0);
        progressDialog.setCancelable(false);

        mItemArray = new ArrayList<>();
        DragListView mDragListView = (DragListView) findViewById(R.id.drag_list_view);
        mDragListView.setDragListListener(new DragListView.DragListListener() {
            @Override
            public void onItemDragStarted(int position) {
//                Toast.makeText(AddRouteActivity.this, "Start - position: " + position, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onItemDragging(int itemPosition, float x, float y) {

            }

            @Override
            public void onItemDragEnded(int fromPosition, int toPosition) {
                if (fromPosition != toPosition) {
//                    Toast.makeText(AddRouteActivity.this, "End - position: " + toPosition, Toast.LENGTH_SHORT).show();
                    Log.d("List of waypoints", mItemArray.toString());
                }
            }
        });

        mDragListView.setLayoutManager(new LinearLayoutManager(AddRouteActivity.this));
        listAdapter = new ItemAdapter(mItemArray, R.layout.list_item, R.id.image, false);
        mDragListView.setAdapter(listAdapter, true);
        mDragListView.setCanDragHorizontally(false);

        timePicker = findViewById(R.id.route_time);

        activityTitle = findViewById(R.id.title);
        if (getIntent().getExtras()!=null) {
            routeId = getIntent().getExtras().getInt("route_id", -1);
        }

        if (routeId != -1) {
            activityTitle.setText("EDIT ROUTE");
            timePicker.setVisibility(View.GONE);

            AndroidNetworking.post(baseUrl + "/getRoute.php")
                    .setOkHttpClient(NetworkCookies.okHttpClient)
                    .addHeaders("Authorization", accessToken)
                    .addBodyParameter("route_id", String.valueOf(routeId))
                    .setPriority(Priority.MEDIUM)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @RequiresApi(api = Build.VERSION_CODES.M)
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONObject result = response.getJSONObject("result");
                                boolean success = Boolean.parseBoolean(result.get("success").toString());
                                if (success) {
                                    JSONObject data = result.getJSONObject("data");
                                    String[] departureTime = data.get("departure_time").toString().split(":");
                                    timePicker.setHour(Integer.parseInt(departureTime[0]));
                                    timePicker.setMinute(Integer.parseInt(departureTime[1]));
                                    timePicker.setVisibility(View.VISIBLE);
                                    mItemArray.add((new RouteWaypoint(i++, data.get("source").toString(), data.get("sourceLatLong").toString())));
                                    String waypointsString = data.get("waypoints").toString();
                                    if (waypointsString.length()>0) {
                                        String[] waypoints = data.get("waypoints").toString().split("\\|");
                                        for (int j = 0; j < waypoints.length; j++) {
                                            mItemArray.add(new RouteWaypoint(i++, waypoints[j], ""));
                                        }
                                    }
                                    mItemArray.add((new RouteWaypoint(i++, data.get("destination").toString(), data.get("destinationLatLong").toString())));
                                } else {
                                    String message = result.get("message").toString();
                                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                                }
                                listAdapter.notifyDataSetChanged();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(ANError error) {
                            Toast.makeText(getApplicationContext(), error.getErrorBody(), Toast.LENGTH_LONG).show();
                        }
                    });
        }


        FloatingActionButton addMore = findViewById(R.id.add_more);
        addMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                    startActivityForResult(builder.build(AddRouteActivity.this), AUTOCOMPLETE_WAYPOINT);
                } catch (GooglePlayServicesRepairableException e) {
                    Log.e("Exception", e.getMessage());
                } catch (GooglePlayServicesNotAvailableException e) {
                    Log.e("Exception", e.getMessage());
                }
            }
        });

        FloatingActionButton saveRoute = findViewById(R.id.save);
        saveRoute.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                progressDialog.setMessage("Saving route...");
                progressDialog.show();
                source = mItemArray.get(0).name;
                destination = mItemArray.get(mItemArray.size()-1).name;
//                sourceLatLong = mItemArray.get(0).latLong;
//                destinationLatLong = mItemArray.get(mItemArray.size()-1).latLong;
                departure_time = String.valueOf(timePicker.getHour()) + ":" + String.valueOf(timePicker.getMinute());

                waypoints = "";
                waypointsLatLong = "";

                for (int i = 1; i<mItemArray.size()-1; i++) {
                    waypoints = waypoints + mItemArray.get(i).name + "|";
                }

                // Origin of route
                String str_origin = "origin=" + source;

                // Destination of route
                String str_dest = "destination=" + destination;

                // Sensor enabled
                String sensor = "sensor=false";
                String mode = "mode=driving";

                String apiKey = "key=" + getResources().getString(R.string.google_maps_key);
                String callback = "callback=initialize";
                // Building the parameters to the web service
                String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode + "&" + apiKey + "&" + callback;

                parameters += "&waypoints="+waypoints;
                // Output format
                String output = "json";

                // Building the url to the web service
                String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

                AndroidNetworking.get(url)
                        .setOkHttpClient(NetworkCookies.okHttpClient)
                        .setPriority(Priority.MEDIUM)
                        .build()
                        .getAsJSONObject(new JSONObjectRequestListener() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    JSONArray result = response.getJSONArray("routes");
                                    if (result.length()>0) {
                                        JSONObject route = result.getJSONObject(0);
                                        JSONArray legs = route.getJSONArray("legs");

                                        sourceLatLong = legs.getJSONObject(0).getJSONObject("start_location").get("lat").toString() + "," + legs.getJSONObject(0).getJSONObject("start_location").get("lng").toString();

                                        destinationLatLong = legs.getJSONObject(legs.length()-1).getJSONObject("end_location").get("lat").toString() + "," + legs.getJSONObject(legs.length()-1).getJSONObject("end_location").get("lng").toString();

                                        for (int i=0; i<legs.length(); i++) {
                                            JSONObject leg = legs.getJSONObject(i);
                                            JSONArray steps = leg.getJSONArray("steps");
                                            for (int j=0; j<steps.length(); j++) {
                                                JSONObject step = steps.getJSONObject(j);
                                                JSONObject startLocation = step.getJSONObject("start_location");
                                                JSONObject endLocation = step.getJSONObject("end_location");
                                                waypointsLatLong = waypointsLatLong + startLocation.get("lat") + "," + startLocation.get("lng");
                                                waypointsLatLong = waypointsLatLong + "|";
                                                waypointsLatLong = waypointsLatLong + endLocation.get("lat") + "," + endLocation.get("lng");
                                                waypointsLatLong = waypointsLatLong + "|";
                                            }
                                        }


                                        if (routeId!=-1) {
                                            AndroidNetworking.post(baseUrl + "/editRoute.php")
                                                    .setOkHttpClient(NetworkCookies.okHttpClient)
                                                    .addHeaders("Authorization", accessToken)
                                                    .addBodyParameter("routeId", String.valueOf(routeId))
                                                    .addBodyParameter("source", source)
                                                    .addBodyParameter("destination", destination)
                                                    .addBodyParameter("waypoints", waypoints)
                                                    .addBodyParameter("sourceLatLong", sourceLatLong)
                                                    .addBodyParameter("destinationLatLong", destinationLatLong)
                                                    .addBodyParameter("waypointsLatLong", waypointsLatLong)
                                                    .addBodyParameter("departure_time", departure_time)
                                                    .setPriority(Priority.MEDIUM)
                                                    .build()
                                                    .getAsJSONObject(new JSONObjectRequestListener() {
                                                        @Override
                                                        public void onResponse(JSONObject response) {
                                                            try {
                                                                JSONObject result = response.getJSONObject("result");
                                                                boolean success = Boolean.parseBoolean(result.get("success").toString());
                                                                if (success) {
                                                                    Intent intent = new Intent(getApplicationContext(), RoutesActivity.class);
                                                                    startActivity(intent);
                                                                    finish();
                                                                } else {
                                                                    String message = result.get("message").toString();
                                                                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                                                                }
                                                            } catch (JSONException e) {
                                                                e.printStackTrace();
                                                            }
                                                        }

                                                        @Override
                                                        public void onError(ANError error) {
                                    Toast.makeText(getApplicationContext(), error.getErrorBody(), Toast.LENGTH_LONG).show();
                                    progressDialog.hide();
                                                        }
                                                    });
                                        }
                                        else {
                                            AndroidNetworking.post(baseUrl + "/addRoute.php")
                                                    .setOkHttpClient(NetworkCookies.okHttpClient)
                                                    .addHeaders("Authorization", accessToken)
                                                    .addBodyParameter("source", source)
                                                    .addBodyParameter("destination", destination)
                                                    .addBodyParameter("waypoints", waypoints)
                                                    .addBodyParameter("sourceLatLong", sourceLatLong)
                                                    .addBodyParameter("destinationLatLong", destinationLatLong)
                                                    .addBodyParameter("waypointsLatLong", waypointsLatLong)
                                                    .addBodyParameter("departure_time", departure_time)
                                                    .setPriority(Priority.MEDIUM)
                                                    .build()
                                                    .getAsJSONObject(new JSONObjectRequestListener() {
                                                        @Override
                                                        public void onResponse(JSONObject response) {
                                                            try {
                                                                JSONObject result = response.getJSONObject("result");
                                                                boolean success = Boolean.parseBoolean(result.get("success").toString());
                                                                if (success) {
                                                                    Intent intent = new Intent(getApplicationContext(), RoutesActivity.class);
                                                                    startActivity(intent);
                                                                    finish();
                                                                } else {
                                                                    String message = result.get("message").toString();
                                                                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                                                                }
                                                            } catch (JSONException e) {
                                                                e.printStackTrace();
                                                            }
                                                        }

                                                        @Override
                                                        public void onError(ANError error) {
                                                            Toast.makeText(getApplicationContext(), error.getErrorBody(), Toast.LENGTH_LONG).show();
                                                            progressDialog.hide();
                                                        }
                                                    });
                                        }

                                    }

                                } catch (JSONException e) {
                                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                                    progressDialog.hide();
                                }
                            }

                            @Override
                            public void onError(ANError error) {
                                Toast.makeText(getApplicationContext(), error.getErrorBody(), Toast.LENGTH_LONG).show();
                                progressDialog.hide();
                            }
                        });

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == AUTOCOMPLETE_WAYPOINT) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(getApplicationContext(), data);
//                Toast.makeText(getApplicationContext(), place.getLatLng().toString(),Toast.LENGTH_LONG).show();
                mItemArray.add(new RouteWaypoint(i++, place.getAddress().toString(), place.getLatLng().latitude+","+place.getLatLng().longitude));
                listAdapter.notifyDataSetChanged();
            }
        }
    }
}
