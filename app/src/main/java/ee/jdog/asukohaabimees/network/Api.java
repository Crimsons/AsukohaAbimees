package ee.jdog.asukohaabimees.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Created by Jakob on 07/14/16.
 */
public interface Api {
    @POST("/")
    Call<SetLocationResponse> sendLocation(@Body LocationRequest locationRequest);
}