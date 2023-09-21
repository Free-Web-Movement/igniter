package io.github.freewebmovement.igniter.connection;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class ReAPI implements Callback<Quota> {

    static String BASE_URL = "https://games.yikuaijiasu.top/";
    public Quota quota;
    OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

    public void start(String username, String password) {
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build();

        APIService apiService = retrofit.create(APIService.class);

        Call<Quota> quotaCall = apiService.getQuota(username, password);
        quotaCall.enqueue(this);
    }

    @Override
    public void onResponse(Call<Quota> call, Response<Quota> response) {
        if (response.isSuccessful()) {
            quota = response.body();
        } else {
            System.out.println(response.errorBody());
        }
    }

    @Override
    public void onFailure(Call<Quota> call, Throwable t) {
        t.printStackTrace();
    }
}
