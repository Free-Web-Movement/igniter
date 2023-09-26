package io.github.freewebmovement.igniter;

import androidx.test.runner.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.freewebmovement.igniter.connection.API;
import io.github.freewebmovement.igniter.models.Server;

@RunWith(AndroidJUnit4.class)

public class APITest {

    @Test
    public void shouldGetServer() throws JSONException {
        API api = new API();
        Server[] servers = api.server();
        assert (servers.length >= 1);

    }
    @Test
    public void shouldGetQuota() throws JSONException {
        String username = "sammy";
        String password = "1234";
        API api = new API();
        String quotaStr = api.quota(username, password);
        JSONObject quotaJSON = new JSONObject(quotaStr);
        assert (quotaJSON.getString("username").equals(username));
        assert (quotaJSON.getInt("quota") == 0);
        assert (quotaJSON.getInt("upload") == 0);
        assert (quotaJSON.getInt("download") == 0);
    }
}
