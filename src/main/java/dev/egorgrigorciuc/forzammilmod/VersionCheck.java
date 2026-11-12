package dev.egorgrigorciuc.forzammilmod;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure comparison of plugin, resource pack and BetterModel versions. No Bukkit types. */
public final class VersionCheck {

    public enum Status { OK, MISMATCH, UNKNOWN }

    private static final Pattern PACK_FILE = Pattern.compile("forzammilmod-pack-(\\d+\\.\\d+\\.\\d+)\\.zip");
    private static final Pattern SEMVER = Pattern.compile("\\d+\\.\\d+\\.\\d+");

    private VersionCheck() {
    }

    /** Version from a URL ending in forzammilmod-pack-X.Y.Z.zip, or "" if it doesn't. */
    public static String packVersionFromUrl(String url) {
        if (url == null) return "";
        Matcher m = PACK_FILE.matcher(url);
        String last = "";
        while (m.find()) last = m.group(1);
        return last;
    }

    /** Pack must carry exactly the plugin's version. Blank pack version means we can't tell. */
    public static Status pack(String pluginVersion, String packVersion) {
        if (packVersion == null || packVersion.isBlank() || pluginVersion == null || pluginVersion.isBlank()) {
            return Status.UNKNOWN;
        }
        return sameCore(pluginVersion, packVersion) ? Status.OK : Status.MISMATCH;
    }

    /** BetterModel: compares major.minor.patch only. A missing installed plugin (null/blank) is a mismatch. */
    public static Status betterModel(String pinned, String installed) {
        if (pinned == null || pinned.isBlank()) return Status.UNKNOWN;
        if (installed == null || installed.isBlank()) return Status.MISMATCH;
        return sameCore(pinned, installed) ? Status.OK : Status.MISMATCH;
    }

    /** First major.minor.patch in the BetterModel row of a docs/versions.md table, or "" if none. */
    public static String pinnedFromDocs(String markdown) {
        if (markdown == null) return "";
        for (String line : markdown.split("\\R")) {
            String t = line.trim();
            if (!t.startsWith("|")) continue;
            String[] cells = t.split("\\|");
            if (cells.length < 3 || !cells[1].trim().equalsIgnoreCase("BetterModel")) continue;
            Matcher m = SEMVER.matcher(cells[2]);
            if (m.find()) return m.group();
        }
        return "";
    }

    private static boolean sameCore(String a, String b) {
        int[] x = core(a);
        int[] y = core(b);
        return Arrays.equals(x, y);
    }

    private static int[] core(String v) {
        int[] p = Versions.parse(v);
        int[] out = new int[3];
        System.arraycopy(p, 0, out, 0, Math.min(3, p.length));
        return out;
    }
}
