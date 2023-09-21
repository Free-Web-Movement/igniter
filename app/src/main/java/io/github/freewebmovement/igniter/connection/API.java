package io.github.freewebmovement.igniter.connection;


import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class API {

    static String BASE_URL = "http://games.yikuaijiasu.top/";
    OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

    Retrofit retrofit = new Retrofit.Builder()
            .addConverterFactory(ScalarsConverterFactory.create())
            .client(httpClient.build())
            .baseUrl(BASE_URL)
            .build();
    APIService apiService = retrofit.create(APIService.class);

    public Quota getQuota(String username, String password) {
        Call<Quota> quotaCall = apiService.getQuota(username, password);
        try {
            Response<Quota> response = quotaCall.execute();
            Quota quota = response.body();
            return quota;
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return null;
    }

}
