package io.github.freewebmovement.igniter;

import androidx.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.freewebmovement.igniter.connection.API;
import io.github.freewebmovement.igniter.connection.Quota;

@RunWith(AndroidJUnit4.class)

public class APITest {

    private CountDownLatch lock = new CountDownLatch(1);

    @Test
    public void shouldGetQuota() throws InterruptedException, JSONException {
        String username = "sammy";
        String password = "1234";
        API api = new API();
        String quotaStr = api.quota(username, password);
        JSONObject quota = new JSONObject(quotaStr);
        assert (quota != null);
        assert (quota.getString("username").equals(username));
        assert (quota.getInt("quota") == 0);
        assert (quota.getInt("upload") == 0);
        assert (quota.getInt("download") == 0);
    }
}
