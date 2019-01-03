package com.gotobus;

class BusRoute {
    int id;
    String source, destination,  sourceLatLong, destinationLatLong, departureTime;

    public BusRoute(int id, String source, String destination, String sourceLatLong, String destinationLatLong, String departureTime) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.sourceLatLong = sourceLatLong;
        this.destinationLatLong = destinationLatLong;
        this.departureTime = departureTime;
    }
}
