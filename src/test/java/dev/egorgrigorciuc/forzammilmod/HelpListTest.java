package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelpListTest {

    private static List<String> ids(boolean owner) {
        return HelpList.visibleTo(owner).stream().map(HelpList.Entry::id).toList();
    }

    @Test
    void everyoneSeesNoOwnerCommands() {
        List<String> ids = ids(false);
        for (String owner : List.of("vehicle-spawn", "vehicle-remove", "update", "reload")) {
            assertFalse(ids.contains(owner), owner);
        }
        for (String open : List.of("help", "guide", "emote-wave", "emote-greeting", "emote-stop", "music", "vehicle-list", "pack", "version")) {
            assertTrue(ids.contains(open), open);
        }
    }

    @Test
    void ownerSeesEverything() {
        assertEquals(HelpList.ALL.size(), ids(true).size());
        assertTrue(ids(true).containsAll(List.of("vehicle-spawn", "vehicle-remove", "update", "reload")));
    }

    @Test
    void debugStaysHidden() {
        for (HelpList.Entry e : HelpList.ALL) {
            assertFalse(e.command().contains("debug"), e.command());
            assertFalse(e.id().contains("debug"), e.id());
        }
    }

    @Test
    void idsAreUnique() {
        Set<String> seen = new HashSet<>();
        for (HelpList.Entry e : HelpList.ALL) assertTrue(seen.add(e.id()), e.id());
    }

    @Test
    void greetingIdIsFilledIn() {
        HelpList.Entry g = HelpList.ALL.stream().filter(e -> e.id().equals("emote-greeting")).findFirst().orElseThrow();
        assertEquals("/emote hello", HelpList.commandText(g, "hello"));
        assertEquals("/fmod help", HelpList.commandText(HelpList.ALL.get(0), "hello"));
    }

    @Test
    void everyEntryHasTextInBothLanguages() {
        for (String f : new String[] {"lang/en.yml", "lang/ru.yml"}) {
            YamlConfiguration y = resource(f);
            assertTrue(y.isString("help.header"), f);
            for (HelpList.Entry e : HelpList.ALL) assertTrue(y.isString(HelpList.langKey(e)), f + " " + HelpList.langKey(e));
            for (String k : GuideService.LINE_KEYS) assertTrue(y.isString(k), f + " " + k);
            assertTrue(y.isString("guide.header"), f);
        }
    }

    @Test
    void placeholdersMatchBetweenLanguages() {
        YamlConfiguration en = resource("lang/en.yml");
        YamlConfiguration ru = resource("lang/ru.yml");
        Pattern p = Pattern.compile("\\{([a-z]+)}");
        for (String k : en.getKeys(true)) {
            if (!en.isString(k)) continue;
            String ruText = ru.getString(k);
            if (ruText == null) continue;
            assertEquals(holders(p, en.getString(k)), holders(p, ruText), "placeholders of " + k);
        }
    }

    private static Set<String> holders(Pattern p, String s) {
        Set<String> out = new HashSet<>();
        Matcher m = p.matcher(s);
        while (m.find()) out.add(m.group(1));
        out.remove("n"); // plural forms may spell the number out
        return out;
    }

    private static YamlConfiguration resource(String path) {
        var in = HelpListTest.class.getClassLoader().getResourceAsStream(path);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
    }
}
