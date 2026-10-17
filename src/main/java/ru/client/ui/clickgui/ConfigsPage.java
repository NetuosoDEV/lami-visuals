package ru.client.ui.clickgui;

import ru.client.core.I18n;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import ru.client.core.ClientMod;
import ru.client.core.config.ConfigManager;
import ru.client.modules.impl.hud.Notifications;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.RenderUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ConfigsPage extends Page {

    private static final float ROW = 32, ROW_GAP = 4, CTRL = 20;

    private final Ui.TextField field = new Ui.TextField("Имя нового конфига", 24).onEnter(this::create);
    private List<String> cache;
    private final Map<String, Long> modified = new HashMap<>();
    private long cacheAt;

    private ConfigManager cm() { return ClientMod.getInstance().getConfigManager(); }

    List<String> configs() {
        if (cache == null || System.currentTimeMillis() - cacheAt > 2000) {
            cache = cm().list();
            modified.clear();
            for (String n : cache) modified.put(n, cm().lastModified(n));
            cacheAt = System.currentTimeMillis();
        }
        return cache;
    }

    void refresh() { cache = null; }

    private void create() {
        String name = ConfigManager.sanitize(field.text);
        if (name.isEmpty()) { field.error(); return; }
        if (cm().create(name)) {
            Notifications.show(I18n.tr("Конфиг создан: %s", name), Ui.SUCCESS);
            field.text = "";
            field.focused = false;
        } else {
            field.error();
            Notifications.show(I18n.tr("Конфиг «%s» уже есть", name), Ui.DANGER);
        }
        refresh();
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, float x, float y, float w, float h, int mx, int my) {
        hits.clear();
        ConfigManager cm = cm();

        float bw = Ui.buttonWidth(I18n.tr("Создать"), Icons.Glyph.PLUS);
        field.render(ctx, x, y, w - bw - 6, CTRL, null, mx, my);
        Ui.button(ctx, hits, "cfg.create", I18n.tr("Создать"), Icons.Glyph.PLUS, x + w - bw, y, bw, CTRL, Ui.Kind.PRIMARY, mx, my, this::create);

        List<String> list = configs();
        float ly = y + CTRL + 22, footer = 28, lh = y + h - footer - 8 - ly;
        Ui.label(ctx, I18n.tr("Сохранённые · %d", list.size()), x, ly - 12, Ui.faint());
        float scroll = scroll(list.size() * (ROW + ROW_GAP), lh);
        ctx.enableScissor((int) x - 1, (int) ly, (int) Math.ceil(x + w + 1), (int) Math.ceil(ly + lh));
        boolean inList = Ui.inside(mx, my, x, ly, w, lh);
        int lmx = inList ? mx : -10000, lmy = inList ? my : -10000;
        float ry = ly - scroll;
        for (String name : list) {
            if (ry + ROW >= ly && ry <= ly + lh) row(ctx, cm, name, x, ry, w, lmx, lmy);
            ry += ROW + ROW_GAP;
        }
        ctx.disableScissor();

        float fy = y + h - footer + 8;
        float ow = Ui.buttonWidth(I18n.tr("Открыть папку"), Icons.Glyph.FOLDER);
        Ui.button(ctx, hits, "cfg.folder", I18n.tr("Открыть папку"), Icons.Glyph.FOLDER, x, fy, ow, CTRL, Ui.Kind.GHOST, mx, my, cm::openFolder);
        Fonts.draw(ctx, I18n.tr("Автосохранение — в активный конфиг"), x + ow + 10, fy + 6, Ui.faint());
    }

    private void row(GuiGraphicsExtractor ctx, ConfigManager cm, String name, float x, float y, float w, int mx, int my) {
        boolean active = name.equals(cm.getCurrent());
        boolean hover = Ui.inside(mx, my, x, y, w, ROW);
        float ha = Ui.anim("cfg.row." + name, hover);
        RenderUtils.drawRoundedRect(ctx, x, y, w, ROW, Ui.RADIUS, ColorUtils.lerp(Ui.surface(), Ui.surfaceHover(), ha));
        RenderUtils.drawRoundedOutline(ctx, x, y, w, ROW, Ui.RADIUS, 1, ColorUtils.lerp(Ui.LINE, Ui.LINE_HI, ha));
        if (active) RenderUtils.drawRoundedRect(ctx, x, y + 8, 2, ROW - 16, 1, Ui.accent());

        float bx = x + w - 6, by = y + (ROW - CTRL) / 2f;
        if (!name.equals(ConfigManager.DEFAULT)) {
            bx -= CTRL;
            Ui.iconButton(ctx, hits, "cfg.del." + name, Icons.Glyph.TRASH, bx, by, CTRL, false, Ui.DANGER, mx, my, () -> {
                cm.delete(name);
                Notifications.show(I18n.tr("Конфиг удалён: %s", name), Ui.DANGER);
                refresh();
            });
            bx -= 4;
        }
        bx -= CTRL;
        Ui.iconButton(ctx, hits, "cfg.save." + name, Icons.Glyph.SAVE, bx, by, CTRL, false, Ui.accent(), mx, my, () -> {
            if (name.equals(cm.getCurrent())) cm.save(); else cm.save(name);
            Notifications.show(I18n.tr("Сохранено в «%s»", name), Ui.SUCCESS);
            refresh();
        });
        bx -= 4;
        if (!active) {
            float lw = Ui.buttonWidth(I18n.tr("Загрузить"), null);
            bx -= lw;
            Ui.button(ctx, hits, "cfg.load." + name, I18n.tr("Загрузить"), null, bx, by, lw, CTRL, Ui.Kind.GHOST, mx, my, () -> {
                cm.switchTo(name);
                Notifications.show(I18n.tr("Загружен конфиг: %s", name), Ui.accent());
                refresh();
            });
            bx -= 4;
        }

        float tx = x + 12, maxW = bx - 8 - tx;
        Fonts.draw(ctx, Fonts.trim(name, (int) maxW), tx, y + 6, active ? Ui.text() : ColorUtils.lerp(Ui.dim(), Ui.text(), 0.5f + 0.5f * ha));
        long mod = modified.getOrDefault(name, 0L);
        String sub = (active ? I18n.tr("активный · ") : "") + (mod > 0 ? I18n.tr("изменён %s", Ui.ago(mod)) : I18n.tr("ещё не сохранён"));
        Ui.label(ctx, Fonts.trim(sub, (int) (maxW / 0.85f)), tx, y + 18, Ui.faint());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (field.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, boolean ctrl) { return field.keyPressed(key, ctrl); }

    @Override
    public boolean charTyped(char c) { return field.charTyped(c); }

    @Override
    public boolean typing() { return field.focused; }
}
