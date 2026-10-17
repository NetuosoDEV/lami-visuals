package ru.client.ui.alt;

import ru.client.core.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import ru.client.core.alt.AltManager;
import ru.client.modules.impl.hud.Notifications;
import ru.client.ui.clickgui.Icons;
import ru.client.ui.clickgui.Page;
import ru.client.ui.clickgui.Ui;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.RenderUtils;

import java.util.List;
import java.util.Random;

public final class AltsPage extends Page {

    private static final float ROW = 30, ROW_GAP = 4, CTRL = 20;
    private static final int STAR = 0xFFE5B84D;

    private final Random random = new Random();
    private final Ui.TextField field = new Ui.TextField("Ник: 3–16 символов, A–Z 0–9 _", 16).onEnter(this::add);

    private static AltManager am() { return AltManager.get(); }

    private void add() {
        String err = am().add(field.text);
        if (err != null) {
            field.error();
            Notifications.show(err, Ui.DANGER);
            return;
        }
        Notifications.show(I18n.tr("Альт добавлен: %s", field.text.trim()), Ui.SUCCESS);
        field.text = "";
        field.focused = false;
    }

    private void login(AltManager.Alt alt) {
        am().login(alt);
        Notifications.show(I18n.tr(Minecraft.getInstance().level != null ? "Аккаунт: %s (со следующего входа)" : "Аккаунт: %s", alt.name()), Ui.accent());
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, float x, float y, float w, float h, int mx, int my) {
        hits.clear();
        AltManager am = am();
        boolean alt = am.altActive();

        float ch = 36;
        RenderUtils.drawRoundedRect(ctx, x, y, w, ch, Ui.RADIUS, Ui.surface());
        RenderUtils.drawRoundedOutline(ctx, x, y, w, ch, Ui.RADIUS, 1, Ui.LINE);
        String current = am.currentName();
        if (alt) Ui.letterAvatar(ctx, current, x + 9, y + 8, 20);
        else Ui.face(ctx, Minecraft.getInstance().getGameProfile(), x + 9, y + 8, 20);
        float btnW = alt ? Ui.buttonWidth(I18n.tr("Вернуть основной"), Icons.Glyph.SWAP) : 0;
        float nameMax = x + w - 10 - btnW - 10 - (x + 38);
        Fonts.draw(ctx, Fonts.trim(current, (int) nameMax), x + 38, y + 8, Ui.text());
        Ui.label(ctx, I18n.tr(alt ? "оффлайн-альт" : "аккаунт лаунчера"), x + 38, y + 20, Ui.faint());
        if (alt) {
            Ui.button(ctx, hits, "alt.restore", I18n.tr("Вернуть основной"), Icons.Glyph.SWAP, x + w - 8 - btnW, y + (ch - CTRL) / 2f, btnW, CTRL,
                    Ui.Kind.GHOST, mx, my, () -> {
                        am.restore();
                        Notifications.show(I18n.tr("Вернул аккаунт лаунчера: %s", am.currentName()), Ui.SUCCESS);
                    });
        }

        float ry = y + ch + 10;
        float addW = Ui.buttonWidth(I18n.tr("Добавить"), Icons.Glyph.PLUS);
        float diceX = x + w - addW - 6 - CTRL;
        field.render(ctx, x, ry, diceX - 6 - x, CTRL, null, mx, my);
        Ui.iconButton(ctx, hits, "alt.dice", Icons.Glyph.DICE, diceX, ry, CTRL, false, Ui.accent(), mx, my, () -> {
            field.text = AltManager.randomName(random);
            field.focus();
        });
        Ui.button(ctx, hits, "alt.add", I18n.tr("Добавить"), Icons.Glyph.PLUS, x + w - addW, ry, addW, CTRL, Ui.Kind.PRIMARY, mx, my, this::add);

        List<AltManager.Alt> list = am.list();
        float ly = ry + CTRL + 22, footer = 26, lh = y + h - footer - 6 - ly;
        Ui.label(ctx, I18n.tr("Альты · %d", list.size()), x, ly - 12, Ui.faint());
        if (list.isEmpty()) {
            Fonts.drawCentered(ctx, I18n.tr("Пока пусто — добавь ник, пароль не нужен"), x + w / 2f, ly + Math.min(30, lh / 2f - 4), Ui.faint(), Fonts.REGULAR);
        } else {
            float scroll = scroll(list.size() * (ROW + ROW_GAP), lh);
            ctx.enableScissor((int) x - 1, (int) ly, (int) Math.ceil(x + w + 1), (int) Math.ceil(ly + lh));
            boolean inList = Ui.inside(mx, my, x, ly, w, lh);
            int lmx = inList ? mx : -10000, lmy = inList ? my : -10000;
            float yy = ly - scroll;
            for (AltManager.Alt a : list) {
                if (yy + ROW >= ly && yy <= ly + lh) row(ctx, am, a, x, yy, w, lmx, lmy);
                yy += ROW + ROW_GAP;
            }
            ctx.disableScissor();
        }

        float fy = y + h - footer + 8;
        float ta = Ui.anim("alt.remember", am.remember());
        Ui.toggle(ctx, x, fy + 4, 20, 11, ta);
        String remLabel = I18n.tr("Входить в последний альт при запуске");
        Fonts.draw(ctx, remLabel, x + 28, fy + 5, ColorUtils.lerp(Ui.dim(), Ui.text(), ta));
        hits.add(x, fy, 28 + Fonts.width(remLabel), 18, () -> am.setRemember(!am.remember()));
        String note = I18n.tr(Minecraft.getInstance().level != null ? "сменится при следующем входе" : "только сервера без лицензии");
        float nw = Ui.labelWidth(note);
        if (x + 28 + Fonts.width(remLabel) + 12 + nw < x + w) Ui.label(ctx, note, x + w - nw, fy + 6, Ui.faint());
    }

    private void row(GuiGraphicsExtractor ctx, AltManager am, AltManager.Alt alt, float x, float y, float w, int mx, int my) {
        boolean active = am.isActive(alt);
        boolean hover = Ui.inside(mx, my, x, y, w, ROW);
        float ha = Ui.anim("alt.row." + alt.name(), hover);
        RenderUtils.drawRoundedRect(ctx, x, y, w, ROW, Ui.RADIUS, ColorUtils.lerp(Ui.surface(), Ui.surfaceHover(), ha));
        RenderUtils.drawRoundedOutline(ctx, x, y, w, ROW, Ui.RADIUS, 1, ColorUtils.lerp(Ui.LINE, Ui.LINE_HI, ha));
        if (active) RenderUtils.drawRoundedRect(ctx, x, y + 8, 2, ROW - 16, 1, Ui.accent());

        Ui.letterAvatar(ctx, alt.name(), x + 9, y + 7, 16);

        float by = y + (ROW - CTRL) / 2f, bx = x + w - 5 - CTRL;
        Ui.iconButton(ctx, hits, "alt.del." + alt.name(), Icons.Glyph.TRASH, bx, by, CTRL, false, Ui.DANGER, mx, my, () -> {
            am.remove(alt);
            Notifications.show(I18n.tr("Альт удалён: %s", alt.name()), Ui.DANGER);
        });
        bx -= 4 + CTRL;
        Ui.iconButton(ctx, hits, "alt.fav." + alt.name(), alt.favorite() ? Icons.Glyph.STAR : Icons.Glyph.STAR_OUTLINE, bx, by, CTRL,
                alt.favorite(), STAR, mx, my, () -> am.toggleFavorite(alt));
        bx -= 4;
        if (active) {
            String label = I18n.tr("в игре");
            float lw = Fonts.width(label) + 14;
            bx -= lw;
            Icons.draw(ctx, Icons.Glyph.CHECK, bx + 5, y + ROW / 2f, Ui.accent());
            Fonts.draw(ctx, label, bx + 13, y + ROW / 2f - 4, Ui.dim());
        } else {
            float lw = Ui.buttonWidth(I18n.tr("Войти"), null);
            bx -= lw;
            Ui.button(ctx, hits, "alt.login." + alt.name(), I18n.tr("Войти"), null, bx, by, lw, CTRL, Ui.Kind.GHOST, mx, my, () -> login(alt));
        }

        float tx = x + 32, maxW = bx - 8 - tx;
        Fonts.draw(ctx, Fonts.trim(alt.name(), (int) maxW), tx, y + 5, active ? Ui.text() : ColorUtils.lerp(Ui.dim(), Ui.text(), 0.6f + 0.4f * ha));
        String sub = active ? I18n.tr("сейчас в этом аккаунте")
                : alt.lastUsed() > 0 ? I18n.tr("входил %s", Ui.ago(alt.lastUsed()))
                : I18n.tr("добавлен %s", Ui.ago(alt.added()));
        Ui.label(ctx, Fonts.trim(sub, (int) (maxW / 0.85f)), tx, y + 17, Ui.faint());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        boolean onField = field.mouseClicked(mx, my, button);
        if (button == 0 && hits.click(mx, my)) return true;
        return onField;
    }

    @Override
    public boolean keyPressed(int key, boolean ctrl) { return field.keyPressed(key, ctrl); }

    @Override
    public boolean charTyped(char c) {
        if (!field.focused) return false;
        if (Character.isLetterOrDigit(c) && c < 128 || c == '_') field.charTyped(c);
        return true;
    }

    @Override
    public boolean typing() { return field.focused; }
}
