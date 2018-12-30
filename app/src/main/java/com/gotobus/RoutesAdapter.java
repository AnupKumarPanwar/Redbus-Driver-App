package com.gotobus;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.woxthebox.draglistview.DragItemAdapter;

import java.util.ArrayList;

class RoutesAdapter extends RecyclerView.Adapter<RoutesAdapter.MyViewHolder> {

    Context context;
    ArrayList<BusRoute> busRoutes;

    public RoutesAdapter(Context context, ArrayList<BusRoute> busRoutes) {
        this.context = context;
        this.busRoutes = busRoutes;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.bus_route, viewGroup, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {
        myViewHolder.source.setText(busRoutes.get(i).source);
        myViewHolder.destination.setText(busRoutes.get(i).destination);
        myViewHolder.editRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, AddRouteActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return busRoutes.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView source, destination;
        ImageView editRoute;
        public MyViewHolder(View itemView) {
            super(itemView);
            source = itemView.findViewById(R.id.source);
            destination = itemView.findViewById(R.id.destination);
            editRoute = itemView.findViewById(R.id.edit_route);
        }
    }
}