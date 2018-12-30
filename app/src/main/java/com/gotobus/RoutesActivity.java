package com.gotobus;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;

public class RoutesActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ArrayList<BusRoute> busRoutes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routes);

        recyclerView = findViewById(R.id.recycler_view);
        busRoutes = new ArrayList<>();
        busRoutes.add(new BusRoute(1, "ISBT 17 Chandigarh", "Kashmiri Gate Delhi", "something", "something", "something"));

        busRoutes.add(new BusRoute(2, "ISBT 17 Chandigarh", "Kashmiri Gate Delhi", "something", "something", "something"));

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);

        RoutesAdapter routesAdapter = new RoutesAdapter(getApplicationContext(), busRoutes);
        recyclerView.setAdapter(routesAdapter);
    }
}
