package io.github.freewebmovement.igniter;

import static junit.framework.TestCase.assertEquals;

import android.content.Context;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.freewebmovement.igniter.persistence.Storage;

@RunWith(AndroidJUnit4.class)
public class StorageTest {
     Context instrumentationContext;

    @Before
    public void setup() {
        instrumentationContext =  androidx.test.core.app.ApplicationProvider.getApplicationContext();
    }
    @Test
    public void shouldInit() {

        Storage storage = new Storage(instrumentationContext);

        String[] paths = {
                storage.getCaCertPath(),
                storage.getCountryMmdbPath(),
                storage.getClashConfigPath()
        };

        int[] ids = {
                R.raw.cacert,
                R.raw.country,
                R.raw.clash_config
        };

        storage.reset();

        for(int i = 0; i < paths.length; i++) {
            String filename = paths[i];
            String rawString = storage.readRawText(ids[i]);
            String content = new String(Storage.read(filename));
            assertEquals(content, rawString);
        }
    }
}
