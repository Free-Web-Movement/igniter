package io.github.freewebmovement.igniter.connection

import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST


interface Quota {
    val username: String
    val quota: Int
    val upload: Int
    val download: Int
}

public interface ApiService {

    @GET("servers")
    fun getServers()

    @FormUrlEncoded
    @POST("user/quota")
    fun getQuota(@Field("username") username: String, @Field("password") password: String): Quota
}

private val BASE_URL =
    "http://games.yikuaijiasu.top"

val retrofit = Retrofit.Builder()
    .addConverterFactory(ScalarsConverterFactory.create())
    .baseUrl(BASE_URL)
    .build()

object ApiObject {
    val retrofitService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
public class API {

    public fun getQuota(username: String, password: String) : Quota {
        return ApiObject.retrofitService.getQuota(username, password);
    }


}
