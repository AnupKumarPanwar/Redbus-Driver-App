package com.gotobus;

public class RouteWaypoint {
    long id;
    String name;
    String latLong;

    public RouteWaypoint(long id, String name, String latLong) {
        this.id = id;
        this.name = name;
        this.latLong = latLong;
    }
}
