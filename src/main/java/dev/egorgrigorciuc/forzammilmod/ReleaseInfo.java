package dev.egorgrigorciuc.forzammilmod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** The bits of a GitHub "latest release" response we care about. */
public record ReleaseInfo(String version, long assetId, String assetName) {

    /** Returns null if the JSON has no tag or no .jar asset. Throws on malformed JSON. */
    public static ReleaseInfo parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonElement tag = root.get("tag_name");
        if (tag == null || tag.isJsonNull()) return null;
        JsonElement assets = root.get("assets");
        if (assets == null || !assets.isJsonArray()) return null;
        JsonArray arr = assets.getAsJsonArray();
        for (JsonElement e : arr) {
            JsonObject a = e.getAsJsonObject();
            JsonElement name = a.get("name");
            JsonElement id = a.get("id");
            if (name != null && id != null && name.getAsString().endsWith(".jar")) {
                return new ReleaseInfo(Versions.strip(tag.getAsString()), id.getAsLong(), name.getAsString());
            }
        }
        return null;
    }
}
