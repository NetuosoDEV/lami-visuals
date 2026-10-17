package ru.client.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;
import ru.client.core.ClientMod;
import ru.client.modules.Module;
import ru.client.modules.ModuleManager;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String DEFAULT = "default";

    private final ModuleManager moduleManager;
    private final Path dir = FabricLoader.getInstance().getConfigDir().resolve(ClientMod.MOD_ID);
    private String current = DEFAULT;

    public ConfigManager(ModuleManager moduleManager) { this.moduleManager = moduleManager; }

    public String getCurrent() { return current; }
    public Path getDir() { return dir; }

    public void save() { save(current); }

    public void load() {
        migrateLegacy();
        try {
            Path active = dir.resolve("active.txt");
            if (Files.exists(active)) {
                String name = sanitize(Files.readString(active, StandardCharsets.UTF_8).trim());
                if (!name.isEmpty() && Files.exists(file(name))) current = name;
            }
        } catch (IOException ignored) {}
        load(current);
    }

    public List<String> list() {
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) { out.add(DEFAULT); return out; }
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString().replaceFirst("\\.json$", ""))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(out::add);
        } catch (IOException e) {
            ClientMod.LOGGER.error("Failed to list configs", e);
        }
        if (!out.contains(DEFAULT)) out.add(0, DEFAULT);
        return out;
    }

    public boolean exists(String name) { return Files.exists(file(sanitize(name))); }

    public long lastModified(String name) {
        try {
            Path f = file(sanitize(name));
            return Files.exists(f) ? Files.getLastModifiedTime(f).toMillis() : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    public void switchTo(String name) {
        name = sanitize(name);
        if (name.isEmpty()) return;
        save(current);
        current = name;
        if (exists(name)) load(name); else save(name);
        writeActive();
    }

    public boolean create(String name) {
        name = sanitize(name);
        if (name.isEmpty() || exists(name)) return false;
        save(current);
        current = name;
        save(name);
        writeActive();
        return true;
    }

    public void delete(String name) {
        name = sanitize(name);
        if (name.equals(DEFAULT)) return;
        try {
            Files.deleteIfExists(file(name));
        } catch (IOException e) {
            ClientMod.LOGGER.error("Failed to delete config {}", name, e);
        }
        if (name.equals(current)) {
            current = DEFAULT;
            if (exists(DEFAULT)) load(DEFAULT);
            writeActive();
        }
    }

    public void openFolder() {
        try {
            Files.createDirectories(dir);
            com.mojang.blaze3d.Blaze3D.openPath(dir);
        } catch (Exception e) {
            ClientMod.LOGGER.error("Failed to open config folder", e);
        }
    }

    public static String sanitize(String name) {
        String s = name == null ? "" : name.trim().replaceAll("[^A-Za-z0-9_\\-а-яА-ЯёЁ ]", "").replace(' ', '_');
        if (s.length() > 24) s = s.substring(0, 24);
        return s.toLowerCase(Locale.ROOT).equals("active") ? "" : s;
    }

    private Path file(String name) { return dir.resolve(name + ".json"); }

    private void writeActive() {
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("active.txt"), current, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ClientMod.LOGGER.warn("Failed to write active config", e);
        }
    }

    public void save(String name) {
        JsonObject root = new JsonObject();
        JsonObject modules = new JsonObject();
        for (Module m : moduleManager.getModules()) modules.add(m.getName(), m.toJson());
        root.add("modules", modules);

        try {
            Files.createDirectories(dir);
            Path tmp = dir.resolve(name + ".json.tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
            try {
                Files.move(tmp, file(name), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailed) {
                Files.move(tmp, file(name), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            ClientMod.LOGGER.error("Failed to save config {}", name, e);
        }
    }

    public void load(String name) {
        Path file = file(name);
        if (!Files.exists(file)) return;
        Module.setSilent(true);
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            if (!root.has("modules")) return;
            JsonObject modules = root.getAsJsonObject("modules");
            for (Module m : moduleManager.getModules()) {
                JsonElement e = modules.get(m.getName());
                if (e != null && e.isJsonObject()) m.fromJson(e.getAsJsonObject());
            }
        } catch (Exception e) {
            ClientMod.LOGGER.error("Failed to load config {}", name, e);
        } finally {
            Module.setSilent(false);
        }
    }

    private void migrateLegacy() {
        Path legacy = dir.resolveSibling("clientvisuals").resolve(DEFAULT + ".json");
        if (Files.exists(file(DEFAULT)) || !Files.exists(legacy)) return;
        try {
            Files.createDirectories(dir);
            Files.copy(legacy, file(DEFAULT));
        } catch (IOException e) {
            ClientMod.LOGGER.warn("Failed to migrate legacy config", e);
        }
    }
}
