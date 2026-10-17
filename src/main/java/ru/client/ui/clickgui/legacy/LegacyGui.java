package ru.client.ui.clickgui.legacy;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import ru.client.core.ClientMod;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.ModuleManager;
import ru.client.modules.settings.Setting;
import ru.client.ui.clickgui.components.SettingComponents;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.RenderUtils;

import java.util.ArrayList;
import java.util.List;

public final class LegacyGui {

    private static String tooltip;

    private final List<LegacyPanel> panels = new ArrayList<>();
    private boolean positioned;

    public LegacyGui() {
        ModuleManager mm = ClientMod.getInstance().getModuleManager();
        for (Category category : Category.values()) {
            if (!category.isModuleTab()) continue;
            LegacyPanel panel = new LegacyPanel(category::getDisplayName);
            if (category == Category.COLORS) {
                for (Module m : mm.getModules(category))
                    for (Setting<?> s : m.getSettings()) panel.add(SettingComponents.create(s));
            } else {
                for (Module m : mm.getModules(category)) panel.add(new LegacyModuleButton(m));
            }
            panels.add(panel);
        }
    }

    static void setTooltip(String text) { tooltip = text; }

    public void init(int width, int height) {
        if (positioned) return;
        float gap = 8;
        float total = panels.size() * LegacyPanel.WIDTH + (panels.size() - 1) * gap;
        float startX = Math.max(10, (width - total) / 2f);
        for (int i = 0; i < panels.size(); i++) panels.get(i).setPosition(startX + i * (LegacyPanel.WIDTH + gap), 30);
        positioned = true;
    }

    public void render(GuiGraphicsExtractor ctx, int mx, int my, float delta, float open, int width, int height) {
        tooltip = null;
        Fonts.setForceVanilla(true);
        try {
            float slide = (1f - open) * -30f;
            for (LegacyPanel p : panels) p.render(ctx, mx, my, delta, slide);

            if (tooltip != null && !tooltip.isEmpty()) {
                Theme t = Theme.get();
                float bw = Fonts.width(tooltip) + 12, bx = width / 2f - bw / 2f, by = height - 26;
                RenderUtils.drawRoundedRect(ctx, bx, by, bw, 16, 4, 0xE0121216);
                RenderUtils.drawRoundedOutline(ctx, bx, by, bw, 16, 4, 1, ColorUtils.withAlpha(t.accentA(), 160));
                Fonts.draw(ctx, tooltip, Math.round(bx + 6), Math.round(by + 4), t.text());
            }
        } finally {
            Fonts.setForceVanilla(false);
        }
    }

    public boolean isOver(double mx, double my) {
        for (LegacyPanel p : panels) if (p.isOver(mx, my)) return true;
        return false;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = panels.size() - 1; i >= 0; i--) {
            LegacyPanel p = panels.get(i);
            if (p.mouseClicked(mx, my, button)) {
                panels.remove(i);
                panels.add(p);
                return true;
            }
        }
        return false;
    }

    public void mouseReleased(double mx, double my, int button) {
        for (LegacyPanel p : panels) p.mouseReleased(mx, my, button);
    }

    public void scroll(double vertical) {
        for (LegacyPanel p : panels) p.scroll((float) vertical * 15f);
    }

    public boolean keyPressed(int key, int scanCode, int modifiers) {
        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).keyPressed(key, scanCode, modifiers)) return true;
        }
        return false;
    }

    public boolean typing() {
        for (LegacyPanel p : panels) if (p.typing()) return true;
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).charTyped(chr, modifiers)) return true;
        }
        return false;
    }
}
