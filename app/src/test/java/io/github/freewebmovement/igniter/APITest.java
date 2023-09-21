package io.github.freewebmovement.igniter;

import androidx.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import io.github.freewebmovement.igniter.connection.API;
import io.github.freewebmovement.igniter.connection.Quota;
import io.github.freewebmovement.igniter.connection.ReAPI;

@RunWith(AndroidJUnit4.class)

public class APITest {

    private CountDownLatch lock = new CountDownLatch(1);

    @Test
    public void shouldGetQuota() throws InterruptedException, JSONException {
        String username = "sammy";
        String password = "1234";
        API api = new API();
        String quotaStr = api.quota(username, password);
        JSONObject quotaJSON = new JSONObject(quotaStr);
        assert (quotaJSON != null);
        assert (quotaJSON.getString("username").equals(username));
        assert (quotaJSON.getInt("quota") == 0);
        assert (quotaJSON.getInt("upload") == 0);
        assert (quotaJSON.getInt("download") == 0);

        ReAPI reAPI = new ReAPI();
        reAPI.start(username, password);
        Quota quota = reAPI.quota;


    }
}
