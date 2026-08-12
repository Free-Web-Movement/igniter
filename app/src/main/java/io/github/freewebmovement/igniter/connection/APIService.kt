package io.github.freewebmovement.igniter.connection

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface APIService {
    @FormUrlEncoded
    @POST("user/quota")
    fun getQuota(@Field("username") username: String, @Field("password") password: String): Call<Quota>
}
