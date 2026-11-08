package dev.egorgrigorciuc.forzammilmod;

/** Pure semantic version compare (major.minor.patch). Leading "v" and any "-suffix" or "+build" are ignored. */
public final class Versions {

    private Versions() {
    }

    public static int[] parse(String v) {
        String s = v == null ? "" : v.trim();
        if (s.startsWith("v") || s.startsWith("V")) s = s.substring(1);
        int cut = s.length();
        for (char c : new char[]{'-', '+'}) {
            int i = s.indexOf(c);
            if (i >= 0 && i < cut) cut = i;
        }
        s = s.substring(0, cut);
        int[] out = new int[3];
        String[] parts = s.split("\\.");
        for (int i = 0; i < 3 && i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                out[i] = 0;
            }
        }
        return out;
    }

    public static int compare(String a, String b) {
        int[] x = parse(a);
        int[] y = parse(b);
        for (int i = 0; i < 3; i++) {
            if (x[i] != y[i]) return Integer.compare(x[i], y[i]);
        }
        return 0;
    }

    public static boolean isNewer(String candidate, String current) {
        return compare(candidate, current) > 0;
    }

    public static String strip(String tag) {
        String s = tag == null ? "" : tag.trim();
        return s.startsWith("v") || s.startsWith("V") ? s.substring(1) : s;
    }
}
