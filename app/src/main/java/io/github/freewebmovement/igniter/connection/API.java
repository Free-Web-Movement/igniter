package io.github.freewebmovement.igniter.connection;


import static io.github.freewebmovement.igniter.constants.API.API_QUOTA_KEY_PASSWORD;
import static io.github.freewebmovement.igniter.constants.API.API_QUOTA_KEY_USERNAME;
import static io.github.freewebmovement.igniter.constants.API.API_QUOTA_PATH;
import static io.github.freewebmovement.igniter.constants.API.BASE_URL;
import static io.github.freewebmovement.igniter.constants.API.SERVER_LIST_URI;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import io.github.freewebmovement.igniter.models.Server;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class API {

    OkHttpClient httpClient = new OkHttpClient();

    public Server[] server() {
        Request request = new Request.Builder()
                .url(SERVER_LIST_URI)
                .build();

        Call call = httpClient.newCall(request);
        try {
            Response response = call.execute();
            assert response.body() != null;
            String body = response.body().string();
            Gson gson = new Gson();
//            JsonParser parser = new JsonParser();
//            JsonObject object = (JsonObject) parser.parse(body);
            return gson.fromJson(body, Server[].class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String quota(String username, String password) {
        RequestBody formBody = new FormBody.Builder()
                .add(API_QUOTA_KEY_USERNAME, username)
                .add(API_QUOTA_KEY_PASSWORD, password)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + API_QUOTA_PATH)
                .post(formBody)
                .build();

        Call call = httpClient.newCall(request);
        try {
            Response response = call.execute();
            assert response.body() != null;
            return response.body().string();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
