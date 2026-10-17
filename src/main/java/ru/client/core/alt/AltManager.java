package ru.client.core.alt;

import ru.client.core.I18n;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.core.UUIDUtil;
import ru.client.core.ClientMod;
import ru.client.mixins.MinecraftUserAccessor;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;

public final class AltManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    private static AltManager instance;

    public static final class Alt {
        private final String name;
        private final long added;
        private long lastUsed;
        private boolean favorite;

        Alt(String name, long added, long lastUsed, boolean favorite) {
            this.name = name;
            this.added = added;
            this.lastUsed = lastUsed;
            this.favorite = favorite;
        }

        public String name() { return name; }
        public long added() { return added; }
        public long lastUsed() { return lastUsed; }
        public boolean favorite() { return favorite; }
    }

    private final Path file = FabricLoader.getInstance().getConfigDir().resolve(ClientMod.MOD_ID).resolve("alts.json");
    private final List<Alt> alts = new ArrayList<>();
    private User original;
    private Alt active;
    private boolean remember;
    private String lastAlt;

    public AltManager() { instance = this; }

    public static AltManager get() { return instance; }

    private static Minecraft mc() { return Minecraft.getInstance(); }

    public User original() {
        if (original == null) original = mc().getUser();
        return original;
    }

    public boolean altActive() { return active != null; }

    public Alt active() { return active; }

    public boolean isActive(Alt alt) { return active == alt; }

    public String currentName() { return mc().getUser().getName(); }

    public GameProfile activeProfile() {
        return active == null ? null : UUIDUtil.createOfflineProfile(active.name);
    }

    public void login(Alt alt) {
        original();
        User user = new User(alt.name, UUIDUtil.createOfflinePlayerUUID(alt.name), "0", Optional.empty(), Optional.empty());
        ((MinecraftUserAccessor) mc()).lamivisuals$setUser(user);
        active = alt;
        alt.lastUsed = System.currentTimeMillis();
        lastAlt = alt.name;
        save();
    }

    public void restore() {
        if (active == null) return;
        ((MinecraftUserAccessor) mc()).lamivisuals$setUser(original());
        active = null;
        lastAlt = null;
        save();
    }

    public void onClientStarted() {
        original();
        if (!remember || lastAlt == null) return;
        Alt a = find(lastAlt);
        if (a != null) login(a);
    }

    public List<Alt> list() {
        List<Alt> out = new ArrayList<>(alts);
        out.sort(Comparator.comparing((Alt a) -> !a.favorite)
                .thenComparing(Comparator.comparingLong((Alt a) -> a.lastUsed).reversed())
                .thenComparing(Comparator.comparingLong((Alt a) -> a.added).reversed()));
        return out;
    }

    public int size() { return alts.size(); }

    public Alt find(String name) {
        for (Alt a : alts) if (a.name.equalsIgnoreCase(name)) return a;
        return null;
    }

    public String add(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (!NAME.matcher(name).matches()) return I18n.tr("Ник: 3–16 символов — латиница, цифры, _");
        if (find(name) != null) return I18n.tr("«%s» уже в списке", name);
        alts.add(new Alt(name, System.currentTimeMillis(), 0, false));
        save();
        return null;
    }

    public void remove(Alt alt) {
        if (alt == active) restore();
        alts.remove(alt);
        save();
    }

    public void toggleFavorite(Alt alt) {
        alt.favorite = !alt.favorite;
        save();
    }

    public boolean remember() { return remember; }

    public void setRemember(boolean value) {
        remember = value;
        save();
    }

    public static boolean isValid(String name) { return NAME.matcher(name).matches(); }

    private static final String[] FIRST = {"Lami", "Neon", "Frost", "Shadow", "Lunar", "Velvet", "Crimson", "Silent", "Nova",
            "Echo", "Pixel", "Swift", "Ember", "Azure", "Mystic", "Toxic", "Cosmo", "Rogue", "Hollow", "Static"};
    private static final String[] SECOND = {"Fox", "Wolf", "Byte", "Ghost", "Raven", "Blade", "Moth", "Comet", "Viper", "Owl",
            "Drift", "Storm", "Lynx", "Spark", "Orbit", "Shade", "Hawk", "Flux", "Koi", "Rune"};

    public static String randomName(Random r) {
        for (int attempt = 0; attempt < 20; attempt++) {
            String base = FIRST[r.nextInt(FIRST.length)] + (r.nextInt(4) == 0 ? "_" : "") + SECOND[r.nextInt(SECOND.length)];
            int style = r.nextInt(3);
            String name = switch (style) {
                case 0 -> base + (10 + r.nextInt(90));
                case 1 -> base.toLowerCase(Locale.ROOT) + "_" + (r.nextInt(9) + 1);
                default -> base;
            };
            if (isValid(name)) return name;
        }
        return "Lami" + (1000 + r.nextInt(9000));
    }

    public void load() {
        alts.clear();
        if (!Files.exists(file)) return;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            remember = root.has("remember") && root.get("remember").getAsBoolean();
            lastAlt = root.has("last") && !root.get("last").isJsonNull() ? root.get("last").getAsString() : null;
            if (root.has("alts")) {
                for (JsonElement e : root.getAsJsonArray("alts")) {
                    JsonObject o = e.getAsJsonObject();
                    String name = o.get("name").getAsString();
                    if (!isValid(name) || find(name) != null) continue;
                    alts.add(new Alt(name,
                            o.has("added") ? o.get("added").getAsLong() : 0,
                            o.has("lastUsed") ? o.get("lastUsed").getAsLong() : 0,
                            o.has("favorite") && o.get("favorite").getAsBoolean()));
                }
            }
        } catch (Exception e) {
            ClientMod.LOGGER.error("Failed to load alts", e);
        }
    }

    public void save() {
        JsonObject root = new JsonObject();
        root.addProperty("remember", remember);
        if (lastAlt != null) root.addProperty("last", lastAlt);
        JsonArray arr = new JsonArray();
        for (Alt a : alts) {
            JsonObject o = new JsonObject();
            o.addProperty("name", a.name);
            o.addProperty("added", a.added);
            o.addProperty("lastUsed", a.lastUsed);
            o.addProperty("favorite", a.favorite);
            arr.add(o);
        }
        root.add("alts", arr);
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("alts.json.tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailed) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            ClientMod.LOGGER.error("Failed to save alts", e);
        }
    }
}
