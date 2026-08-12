package io.github.freewebmovement.igniter

import io.github.freewebmovement.igniter.persistence.ClashConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ClashTest {
    @Test
    fun shouldParseYaml() {
        val filename = "./src/test/java/io/github/freewebmovement/igniter/data/scratch.yml"
        val f = File(filename)
        val cc = ClashConfig(f.absolutePath)
        val f1 = File("$filename.tmp")
        cc.save(f1.absolutePath)
        val cc1 = ClashConfig(f1.absolutePath)
        assertEquals(cc1.data, cc.data)
        var port = cc1.getPort()
        var trojanPort = cc1.getTrojanPort()
        assertEquals(1180, port)
        assertEquals(1080, trojanPort)
        cc1.setPort(1181)
        cc1.setTrojanPort(1081)
        port = cc1.getPort()
        trojanPort = cc1.getTrojanPort()
        assertEquals(1181, port)
        assertEquals(1081, trojanPort)
        assert(f1.delete())
    }
}
