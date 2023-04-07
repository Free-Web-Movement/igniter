package io.github.freewebmovement.igniter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import io.github.freewebmovement.igniter.persistence.ClashConfig;
import io.github.freewebmovement.igniter.persistence.Storage;

public class ClashTest {
    @Test
    public void shouldParseYaml() throws IOException {
        String filename = "./src/test/java/io/github/freewebmovement/igniter/data/scratch.yml";
        File f = new File(filename);
        String text = new String(Storage.read(f.getAbsolutePath()));
        ClashConfig cc = new ClashConfig(f.getAbsolutePath());
        File f1 = new File(filename + ".tmp");
        cc.save(f1.getAbsolutePath());
        String text1 = new String(Storage.read(f1.getAbsolutePath()));
        ClashConfig cc1 = new ClashConfig(f1.getAbsolutePath());
        assertEquals(cc1.data, cc.data);
        int port = cc1.getPort();
        int trojanPort = cc1.getTrojanPort();
        assertTrue(port == 1180);
        assertTrue(trojanPort == 1080);
        cc1.setPort(1181);
        cc1.setTrojanPort(1081);
        port = cc1.getPort();
        trojanPort = cc1.getTrojanPort();
        assertTrue(port == 1181);
        assertTrue(trojanPort == 1081);
        f1.delete();
    }
}