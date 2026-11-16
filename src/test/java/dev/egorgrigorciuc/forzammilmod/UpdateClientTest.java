package dev.egorgrigorciuc.forzammilmod;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class UpdateClientTest {
    private HttpServer server;
    private String base;
    private final AtomicReference<String> cdnAuth = new AtomicReference<>("unset");
    private final AtomicReference<String> apiAuth = new AtomicReference<>();
    private volatile int assetStatus = 302;
    private volatile String latestTag = "v0.2.0";

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        base = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/repos/" + UpdateClient.REPO + "/releases/latest", ex -> {
            apiAuth.set(ex.getRequestHeaders().getFirst("Authorization"));
            byte[] b = ("{\"tag_name\":\"" + latestTag + "\",\"assets\":[{\"id\":7,\"name\":\"forzammilmod-0.2.0.jar\"}]}")
                    .getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        server.createContext("/repos/" + UpdateClient.REPO + "/releases/assets/7", ex -> {
            if (assetStatus == 302) {
                ex.getResponseHeaders().add("Location", "/cdn/file");
                ex.sendResponseHeaders(302, -1);
            } else {
                ex.sendResponseHeaders(assetStatus, -1);
            }
            ex.close();
        });
        server.createContext("/cdn/file", ex -> {
            cdnAuth.set(String.valueOf(ex.getRequestHeaders().getFirst("Authorization")));
            byte[] b = "JARBYTES".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void downloadsAndDropsAuthOnRedirect(@TempDir Path dir) throws Exception {
        UpdateResult r = new UpdateClient(base).checkAndDownload("tok", "0.1.0", dir, "forzammilmod.jar");
        assertEquals(new UpdateResult.Downloaded("0.2.0"), r);
        assertEquals("Bearer tok", apiAuth.get());
        assertEquals("null", cdnAuth.get());
        assertEquals("JARBYTES", Files.readString(dir.resolve("forzammilmod.jar")));
        try (var s = Files.list(dir)) {
            assertEquals(1, s.count());
        }
    }

    @Test
    void upToDate(@TempDir Path dir) {
        assertInstanceOf(UpdateResult.UpToDate.class, new UpdateClient(base).checkAndDownload("tok", "0.2.0", dir, "a.jar"));
        assertFalse(Files.exists(dir.resolve("a.jar")));
    }

    @Test
    void worksWithoutToken(@TempDir Path dir) throws Exception {
        apiAuth.set("unset");
        UpdateResult r = new UpdateClient(base).checkAndDownload("  ", "0.1.0", dir, "a.jar");
        assertEquals(new UpdateResult.Downloaded("0.2.0"), r);
        assertNull(apiAuth.get());
        assertEquals("JARBYTES", Files.readString(dir.resolve("a.jar")));
    }

    @Test
    void non200IsError(@TempDir Path dir) {
        assetStatus = 404;
        UpdateResult r = new UpdateClient(base).checkAndDownload("tok", "0.1.0", dir, "a.jar");
        assertInstanceOf(UpdateResult.Error.class, r);
        assertFalse(Files.exists(dir.resolve("a.jar")));
    }

    @Test
    void connectionFailureIsError(@TempDir Path dir) {
        server.stop(0);
        assertInstanceOf(UpdateResult.Error.class, new UpdateClient(base).checkAndDownload("tok", "0.1.0", dir, "a.jar"));
    }
}
