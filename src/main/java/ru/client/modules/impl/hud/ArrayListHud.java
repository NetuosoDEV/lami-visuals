package ru.client.modules.impl.hud;

import ru.client.core.I18n;
import ru.client.utils.render.Fonts;
import ru.client.core.ClientMod;
import ru.client.modules.Category;
import ru.client.modules.HudModule;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.RenderUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ArrayListHud extends HudModule {

    private final BooleanSetting background = register(new BooleanSetting("Background", true));
    private final BooleanSetting sideBar = register(new BooleanSetting("Side Bar", true));

    private final Map<Module, Animation> anims = new IdentityHashMap<>();
    private final List<Module> order = new ArrayList<>();

    public ArrayListHud() {
        super("ArrayList", "Список включённых модулей", Category.HUD, 10000, 4);
        setToggled(true);
    }

    @Override
    protected void renderHud(GuiGraphicsExtractor ctx, float x, float y, float td) {
        Theme t = Theme.get();
        order.clear();
        for (Module m : ClientMod.getInstance().getModuleManager().getModules()) {
            if (m.isHidden() || m == this) continue;
            Animation a = anims.computeIfAbsent(m, k -> new Animation(10f, m.isToggled() ? 1f : 0f));
            a.setTarget(m.isToggled() ? 1f : 0f);
            if (a.update() > 0.01f) order.add(m);
        }
        order.sort(Comparator.comparingInt((Module m) -> Fonts.width(m.getDisplayName())).reversed());

        float lineH = 11;
        float maxW = 40;
        for (Module m : order) maxW = Math.max(maxW, Fonts.width(m.getDisplayName()) + 10);
        width = maxW;
        float total = 0;
        for (Module m : order) total += lineH * anims.get(m).get();
        height = Math.max(lineH, total);

        boolean right = isRightSide();
        float cy = y;
        int i = 0;
        for (Module m : order) {
            float a = anims.get(m).get();
            String name = m.getDisplayName();
            float w = Fonts.width(name) + 10;
            float slide = (1f - a) * (w + 4) * (right ? 1 : -1);
            float lx = right ? x + width - w + slide : x + slide;
            int col = t.accent(i * 180);
            if (background.isEnabled()) RenderUtils.drawRect(ctx, lx, cy, w, lineH * a, ColorUtils.multAlpha(t.panel(), a));
            if (sideBar.isEnabled()) RenderUtils.drawRect(ctx, right ? lx + w - 1.5f : lx, cy, 1.5f, lineH * a, ColorUtils.multAlpha(col, a));
            if (a > 0.3f) Fonts.draw(ctx, name, Math.round(lx + 5), Math.round(cy + 2), ColorUtils.multAlpha(col, a), false);
            cy += lineH * a;
            i++;
        }
    }
}
