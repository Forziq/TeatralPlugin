package dev.egorgrigorciuc.forzammilmod;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

/** Parsed pack.properties: public URL plus SHA-1 of the resource pack zip. */
public record PackProperties(String url, byte[] sha1) {

    private static final Pattern HEX40 = Pattern.compile("[0-9a-fA-F]{40}");

    public static Optional<PackProperties> parse(InputStream in) {
        if (in == null) return Optional.empty();
        Properties p = new Properties();
        try (InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException e) {
            return Optional.empty();
        }
        String url = p.getProperty("url", "").trim();
        String sha = p.getProperty("sha1", "").trim();
        if (url.isEmpty() || !HEX40.matcher(sha).matches()) return Optional.empty();
        byte[] bytes = new byte[20];
        for (int i = 0; i < 20; i++) {
            bytes[i] = (byte) Integer.parseInt(sha.substring(i * 2, i * 2 + 2), 16);
        }
        return Optional.of(new PackProperties(url, bytes));
    }
}
