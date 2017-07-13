package com.vinhdn.taskcontrol.data;

/**
 * Created by vinh on 7/13/17.
 */

public class UserLocation extends DataImpl{

    private double lat;
    private double lng;

    public UserLocation(double lat, double lng) {
        super();
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    @Override
    public String toData() {
        return "User location: " + "latitude - " + lat + " longitude - " + lng + " " + super.toData();
    }
}
