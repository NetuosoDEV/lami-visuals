package ru.client.modules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.platform.InputConstants;
import ru.client.core.I18n;
import ru.client.modules.settings.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public abstract class Module {

    protected static final Minecraft mc = Minecraft.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();

    private int key;
    private boolean toggled;
    private boolean hidden;

    private static Consumer<Module> toggleListener = m -> {};

    private static boolean silent;

    public static void setToggleListener(Consumer<Module> listener) { toggleListener = listener; }

    public static void setSilent(boolean value) { silent = value; }

    protected Module(String name, String description, Category category) {
        this(name, description, category, -1);
    }

    protected Module(String name, String description, Category category, int key) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.key = key;
    }

    protected <S extends Setting<?>> S register(S setting) {
        settings.add(setting);
        return setting;
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}
    public void onRender2D(GuiGraphicsExtractor context, float tickDelta) {}
    public void onRender3D(PoseStack matrices, Camera camera, float tickDelta) {}
    public void onAttack(Entity target) {}
    public void onKill(LivingEntity target) {}
    public void onKeyPress(int key) {}

    public final void toggle() { setToggled(!toggled); }

    public final void setToggled(boolean state) {
        if (this.toggled == state) return;
        this.toggled = state;
        if (state) onEnable(); else onDisable();
        if (!silent) toggleListener.accept(this);
    }

    protected static boolean nullCheck() {
        return mc.player == null || mc.level == null;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("enabled", toggled);
        obj.addProperty("key", key);
        JsonObject s = new JsonObject();
        for (Setting<?> setting : settings) s.add(setting.getName(), setting.toJson());
        obj.add("settings", s);
        return obj;
    }

    public void fromJson(JsonObject obj) {
        if (obj.has("key")) key = obj.get("key").getAsInt();
        if (obj.has("settings") && obj.get("settings").isJsonObject()) {
            JsonObject s = obj.getAsJsonObject("settings");
            for (Setting<?> setting : settings) {
                JsonElement e = s.get(setting.getName());
                if (e == null) continue;
                try {
                    setting.fromJson(e);
                } catch (Exception ignored) {
                    setting.reset();
                }
            }
        }
        if (obj.has("enabled")) setToggled(obj.get("enabled").getAsBoolean());
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getDisplayName() { return name; }
    public String getDisplayDescription() { return I18n.desc(name, description); }
    public Category getCategory() { return category; }
    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }
    public boolean isToggled() { return toggled; }
    public boolean isHidden() { return hidden; }
    protected void setHidden(boolean hidden) { this.hidden = hidden; }
    public List<Setting<?>> getSettings() { return Collections.unmodifiableList(settings); }
}
