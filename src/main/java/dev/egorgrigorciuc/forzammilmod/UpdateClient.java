package dev.egorgrigorciuc.forzammilmod;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;

/** Blocking HTTP logic for updates. Call from an async thread only. Base URI is injectable for tests. */
public final class UpdateClient {

    public static final String GITHUB_API = "https://api.github.com";
    public static final String REPO = "Forziq/TeatralPlugin";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final int MAX_HOPS = 5;

    private final HttpClient http;
    private final String base;

    public UpdateClient(String base) {
        this(HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build(), base);
    }

    public UpdateClient(HttpClient http, String base) {
        this.http = http;
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    /**
     * Checks the latest release and, if newer than currentVersion, downloads the jar into updateDir/jarName.
     * The repo is public, so token may be blank; a token only raises GitHub's rate limit.
     */
    public UpdateResult checkAndDownload(String token, String currentVersion, Path updateDir, String jarName) {
        try {
            HttpResponse<String> res = http.send(
                    request(base + "/repos/" + REPO + "/releases/latest", token, "application/vnd.github+json").build(),
                    HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return new UpdateResult.Error("GitHub answered " + res.statusCode());
            ReleaseInfo info = ReleaseInfo.parse(res.body());
            if (info == null) return new UpdateResult.Error("release has no jar file");
            if (!Versions.isNewer(info.version(), currentVersion)) return new UpdateResult.UpToDate();

            Files.createDirectories(updateDir);
            Path tmp = Files.createTempFile(updateDir, "download-", ".tmp");
            try {
                String err = download(base + "/repos/" + REPO + "/releases/assets/" + info.assetId(), token, tmp);
                if (err != null) return new UpdateResult.Error(err);
                Files.move(tmp, updateDir.resolve(jarName), StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(tmp);
            }
            return new UpdateResult.Downloaded(info.version());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new UpdateResult.Error("interrupted");
        } catch (Exception e) {
            return new UpdateResult.Error(e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage()));
        }
    }

    /** Returns null on success, otherwise a short reason. Auth header is sent only to the first hop. */
    private String download(String url, String token, Path target) throws IOException, InterruptedException {
        URI uri = URI.create(url);
        boolean first = true;
        for (int hop = 0; hop <= MAX_HOPS; hop++) {
            HttpRequest.Builder rb = first
                    ? request(uri.toString(), token, "application/octet-stream")
                    : HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET();
            HttpResponse<InputStream> res = http.send(rb.build(), HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream in = res.body()) {
                int code = res.statusCode();
                if (code >= 300 && code < 400) {
                    Optional<String> loc = res.headers().firstValue("Location");
                    if (loc.isEmpty()) return "redirect without location";
                    uri = uri.resolve(loc.get());
                    first = false;
                    continue;
                }
                if (code != 200) return "download answered " + code;
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                return null;
            }
        }
        return "too many redirects";
    }

    private static HttpRequest.Builder request(String url, String token, String accept) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", accept)
                .header("User-Agent", "forzammilmod-updater")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET();
        if (token != null && !token.isBlank()) b.header("Authorization", "Bearer " + token.trim());
        return b;
    }
}
