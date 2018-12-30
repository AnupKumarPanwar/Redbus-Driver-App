package com.gotobus;

class BusRoute {
    int id;
    String source, destination, waypoints,  sourceLatLong, destinationLatLong;

    public BusRoute(int id, String source, String destination, String waypoints, String sourceLatLong, String destinationLatLong) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.waypoints = waypoints;
        this.sourceLatLong = sourceLatLong;
        this.destinationLatLong = destinationLatLong;
    }
}
