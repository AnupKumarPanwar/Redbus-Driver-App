package com.gotobus;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

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

import java.util.ArrayList;

public class AddRouteActivity extends AppCompatActivity {

    ArrayList<RouteWaypoint> mItemArray;
    int AUTOCOMPLETE_WAYPOINT = 1;
    long i=0;
    ItemAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_route);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

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

        mItemArray = new ArrayList<>();
        for (i = 0; i < 4; i++) {
            mItemArray.add(new RouteWaypoint((long) i, "Item " + i, "yoyo"));
        }

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

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == AUTOCOMPLETE_WAYPOINT) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(getApplicationContext(), data);
                mItemArray.add(new RouteWaypoint(i++, place.getAddress().toString(), place.getLatLng().toString()));
                listAdapter.notifyDataSetChanged();
            }
        }
    }
}
