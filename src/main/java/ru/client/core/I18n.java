package ru.client.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import ru.client.modules.Modules;
import ru.client.modules.impl.utility.ClickGui;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class I18n {
    private I18n() {}

    public enum Lang { EN, RU, ES }

    public static final String AUTO = "Auto", ENGLISH = "English", RUSSIAN = "Русский", SPANISH = "Español";

    private static final Map<Lang, Map<String, String>> TABLES = new EnumMap<>(Lang.class);
    private static Lang cached = Lang.EN;
    private static String cachedSetting, cachedGame;

    public static void load() {
        for (Lang l : Lang.values()) {
            Map<String, String> map = new HashMap<>();
            String path = "/assets/" + ClientMod.MOD_ID + "/i18n/" + l.name().toLowerCase(Locale.ROOT) + ".json";
            try (InputStream in = I18n.class.getResourceAsStream(path)) {
                if (in != null) {
                    JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> section : root.entrySet()) {
                        if (!section.getValue().isJsonObject()) continue;
                        for (Map.Entry<String, JsonElement> e : section.getValue().getAsJsonObject().entrySet()) {
                            map.put(section.getKey() + "." + e.getKey(), e.getValue().getAsString());
                        }
                    }
                }
            } catch (Exception e) {
                ClientMod.LOGGER.warn("I18n: не удалось прочитать {}", path, e);
            }
            TABLES.put(l, map);
        }
    }

    public static Lang lang() {
        ClickGui g = Modules.get(ClickGui.class);
        String setting = g == null ? AUTO : g.language.get();
        Minecraft mc = Minecraft.getInstance();
        String game = mc == null || mc.options == null ? "en_us" : mc.options.languageCode;
        if (setting.equals(cachedSetting) && game.equals(cachedGame)) return cached;
        cachedSetting = setting;
        cachedGame = game;
        cached = switch (setting) {
            case ENGLISH -> Lang.EN;
            case RUSSIAN -> Lang.RU;
            case SPANISH -> Lang.ES;
            default -> fromGame(game);
        };
        return cached;
    }

    private static Lang fromGame(String code) {
        String c = code.toLowerCase(Locale.ROOT);
        if (c.startsWith("ru") || c.startsWith("uk") || c.startsWith("be") || c.startsWith("kk")) return Lang.RU;
        if (c.startsWith("es")) return Lang.ES;
        return Lang.EN;
    }

    private static String get(Lang l, String key) {
        Map<String, String> t = TABLES.get(l);
        return t == null ? null : t.get(key);
    }

    private static String find(String key) {
        Lang l = lang();
        String v = get(l, key);
        if (v == null && l == Lang.ES) v = get(Lang.EN, key);
        return v;
    }

    public static String tr(String ru, Object... args) {
        String s = lang() == Lang.RU ? ru : find("ui." + ru);
        if (s == null) s = ru;
        return args.length == 0 ? s : String.format(Locale.ROOT, s, args);
    }

    public static String desc(String id, String ru) {
        if (lang() == Lang.RU) return ru;
        String v = find("desc." + id);
        return v == null ? ru : v;
    }
}
