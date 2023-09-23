package io.github.freewebmovement.igniter.connection;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface APIService {
    @FormUrlEncoded
    @POST("user/quota")
    Call<Quota> getQuota(@Field("username") String username, @Field("password") String password);

}
