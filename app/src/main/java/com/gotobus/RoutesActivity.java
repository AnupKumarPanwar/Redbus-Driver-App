package com.gotobus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class RoutesActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ArrayList<BusRoute> busRoutes;
    String baseUrl;
    SharedPreferences sharedPreferences;
    String PREFS_NAME = "MyApp_Settings";
    String accessToken;

    ProgressBar progressBar;
    TextView noRoutesAvailable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routes);

        baseUrl = getResources().getString(R.string.base_url);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        accessToken = sharedPreferences.getString("access_token", null);

        progressBar = findViewById(R.id.progress_bar);
        noRoutesAvailable = findViewById(R.id.no_routes_available);
        recyclerView = findViewById(R.id.recycler_view);
        busRoutes = new ArrayList<>();

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);

        final RoutesAdapter routesAdapter = new RoutesAdapter(getApplicationContext(), busRoutes);
        recyclerView.setAdapter(routesAdapter);

        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               Intent intent = new Intent(getApplicationContext(), AddRouteActivity.class);
               startActivity(intent);
            }
        });


        AndroidNetworking.post(baseUrl + "/getRoutes.php")
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
                                progressBar.setVisibility(View.GONE);
                                JSONArray data = result.getJSONArray("data");
                                if (data.length()==0) {
                                    noRoutesAvailable.setVisibility(View.VISIBLE);
                                }
                                for (int i = 0; i<data.length(); i++) {
                                    JSONObject route = data.getJSONObject(i);
                                            busRoutes.add(new BusRoute(Integer.parseInt(route.get("id").toString()), route.get("source").toString(), route.get("destination").toString(), route.get("sourceLatLong").toString(), route.get("destinationLatLong").toString(), route.get("departure_time").toString()));
                                }
                            } else {
                                String message = result.get("message").toString();
                                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                            }
                            routesAdapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ANError error) {
                        Toast.makeText(getApplicationContext(), error.getErrorBody(), Toast.LENGTH_LONG).show();
                    }
                });

//        busRoutes.add(new BusRoute(1, "ISBT 17 Chandigarh", "Kashmiri Gate Delhi", "something", "something", "something"));
//
//        busRoutes.add(new BusRoute(2, "ISBT 17 Chandigarh", "Kashmiri Gate Delhi", "something", "something", "something"));

    }
}
