package ee.jdog.asukohaabimees.network;

/**
 * Created by Jakob on 07/14/16.
 */
public class LocationRequest {
    final double latitude;
    final double longitude;
    final float velocity;

    public LocationRequest(double latitude, double longitude, float velocity) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.velocity = velocity;
    }
}