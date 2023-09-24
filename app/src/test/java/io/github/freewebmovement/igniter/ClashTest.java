package io.github.freewebmovement.igniter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import io.github.freewebmovement.igniter.persistence.ClashConfig;

public class ClashTest {
    @Test
    public void shouldParseYaml() throws IOException {
        String filename = "./src/test/java/io/github/freewebmovement/igniter/data/scratch.yml";
        File f = new File(filename);
        ClashConfig cc = new ClashConfig(f.getAbsolutePath());
        File f1 = new File(filename + ".tmp");
        cc.save(f1.getAbsolutePath());
        ClashConfig cc1 = new ClashConfig(f1.getAbsolutePath());
        assertEquals(cc1.data, cc.data);
        int port = cc1.getPort();
        int trojanPort = cc1.getTrojanPort();
        assertEquals(1180, port);
        assertEquals(1080, trojanPort);
        cc1.setPort(1181);
        cc1.setTrojanPort(1081);
        port = cc1.getPort();
        trojanPort = cc1.getTrojanPort();
        assertEquals(1181, port);
        assertEquals(1081, trojanPort);
        assert f1.delete();
    }
}