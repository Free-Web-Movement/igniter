package io.github.freewebmovement.igniter.connection;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.OkHttpClient;
import okhttp3.Response;


public class API {

    static String BASE_URL = "https://games.yikuaijiasu.top/";
    public Quota quota;
    OkHttpClient httpClient = new OkHttpClient();

        public String quota(String username , String password) {
        RequestBody formBody = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "user/quota")
                .post(formBody)
                .build();

        Call call = httpClient.newCall(request);
            try {
                Response response = call.execute();
                return response.body().string();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

}
