package ru.client.ui.clickgui;

import ru.client.core.I18n;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import ru.client.core.ClientMod;
import ru.client.core.alt.AltManager;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.ModuleManager;
import ru.client.modules.impl.utility.ClickGui;
import ru.client.modules.settings.Setting;
import ru.client.ui.UiSounds;
import ru.client.ui.alt.AltsPage;
import ru.client.ui.Logo;
import ru.client.ui.clickgui.components.Component;
import ru.client.ui.clickgui.components.SettingComponents;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.RenderUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ModernGui {

    private static final float MAX_W = 600, MAX_H = 360, R = 8, HEADER = 44, GAP = 8, ITEM_H = 22;
    private static final Category[] NAV = {Category.COMBAT, Category.RENDER, Category.HUD, Category.UTILITY,
            Category.COLORS, Category.CONFIGS, Category.ACCOUNTS};

    private final ClickGuiScreen screen;
    private final Map<Category, List<ModuleCard>> cards = new EnumMap<>(Category.class);
    private final List<ModuleCard> allCards = new ArrayList<>();
    private final ConfigsPage configs = new ConfigsPage();
    private final AltsPage alts = new AltsPage();
    private final Ui.Hits hits = new Ui.Hits();
    private final Ui.TextField search = new Ui.TextField("Поиск", 32).hint("Ctrl+F");
    private final List<Component> uiSettings = new ArrayList<>();

    private float vw, vh;
    private float winX = Float.NaN, winY, winW, winH, sbW;
    private boolean compact;
    private boolean dragging;
    private float dragX, dragY;
    private float renderY;

    private Category selected = Category.COMBAT;
    private final Animation navAnim = new Animation(20f, 0f);
    private boolean navSnapped;
    private final Animation scrollAnim = new Animation(16f, 0f);
    private float scrollTarget;
    private String lastSearch = "";

    private boolean settingsOpen;
    private float popX, popY, popH;
    private static final float POP_W = 200;

    private ModuleCard hoveredCard;
    private long hoverSince;
    private String tip;
    private final Animation tipAnim = new Animation(16f, 0f);
    private String lastTip;

    ModernGui(ClickGuiScreen screen) {
        this.screen = screen;
        ModuleManager mm = ClientMod.getInstance().getModuleManager();
        for (Category c : Category.values()) {
            List<ModuleCard> list = new ArrayList<>();
            if (c.isModuleTab()) for (Module m : mm.getModules(c)) list.add(new ModuleCard(m));
            cards.put(c, list);
            allCards.addAll(list);
        }
        ClickGui gui = ClickGui.get();
        if (gui != null) for (Setting<?> s : gui.getSettings()) uiSettings.add(SettingComponents.create(s));
    }

    void init(float vw, float vh) {
        this.vw = vw;
        this.vh = vh;
        layout();
        search.focused = false;
        settingsOpen = false;
        ModuleCard.stopListening();
        configs.refresh();
    }

    void openPage(Category c) { select(c); }

    private void select(Category c) {
        if (c == selected && search.text.isEmpty()) return;
        UiSounds.play(UiSounds.Sfx.TAB_SWITCH);
        selected = c;
        search.text = "";
        search.focused = false;
        lastSearch = "";
        scrollTarget = 0;
        scrollAnim.snap(0);
        ModuleCard.stopListening();
        if (c == Category.CONFIGS) configs.refresh();
    }

    private boolean searching() { return !search.text.isEmpty(); }

    private Page page() {
        if (searching()) return null;
        return selected == Category.CONFIGS ? configs : selected == Category.ACCOUNTS ? alts : null;
    }

    private static boolean inWorld() { return Minecraft.getInstance().level != null; }

    private List<ModuleCard> visibleCards() {
        if (!searching()) return cards.get(selected);
        String q = search.text.toLowerCase(Locale.ROOT);
        List<ModuleCard> out = new ArrayList<>();
        for (ModuleCard c : allCards) {
            if (c.module.getCategory() == Category.COLORS) continue;
            if (c.module.getDisplayName().toLowerCase(Locale.ROOT).contains(q)
                    || c.module.getName().toLowerCase(Locale.ROOT).contains(q)
                    || c.module.getDisplayDescription().toLowerCase(Locale.ROOT).contains(q)) out.add(c);
        }
        return out;
    }

    private void layout() {
        winW = Math.min(MAX_W, vw - 32);
        winH = Math.min(MAX_H, vh - 32);
        compact = winW < 480 || winH < 300;
        sbW = compact ? 44 : 148;
        if (Float.isNaN(winX)) {
            winX = (vw - winW) / 2f;
            winY = (vh - winH) / 2f;
        }
        winX = Mth.clamp(winX, 0, Math.max(0, vw - winW));
        winY = Mth.clamp(winY, 0, Math.max(0, vh - winH));
    }

    private float navTop(Category c) {
        int i = 0;
        while (NAV[i] != c) i++;
        if (!compact) return i < 4 ? 62 + i * 24 : 182 + (i - 4) * 24;
        return i < 4 ? 50 + i * 26 : 50 + 4 * 26 + 10 + (i - 4) * 26;
    }

    boolean overWindow(double mx, double my) {
        return mx >= winX && mx <= winX + winW && my >= winY && my <= winY + winH;
    }

    private boolean overPopover(double mx, double my) {
        return settingsOpen && Ui.inside(mx, my, popX, popY, POP_W, popH);
    }

    private float contentX() { return winX + sbW + 16; }

    private float contentW() { return winX + winW - 16 - contentX(); }

    void render(GuiGraphicsExtractor ctx, int mx, int my, float delta, float open, float vw, float vh) {
        this.vw = vw;
        this.vh = vh;
        layout();
        if (dragging) {
            winX = Mth.clamp(mx - dragX, 0, Math.max(0, vw - winW));
            winY = Mth.clamp(my - dragY, 0, Math.max(0, vh - winH));
        }
        if (!search.text.equals(lastSearch)) {
            lastSearch = search.text;
            scrollTarget = 0;
            scrollAnim.snap(0);
        }

        float x = winX, w = winW, h = winH;
        float y = winY + (1f - open) * 8f;
        renderY = y;
        hits.clear();
        tip = null;

        Component.setFlat(true);
        try {
            RenderUtils.drawGlow(ctx, x, y, w, h, R, 12, ColorUtils.withAlpha(0x000000, (int) (120 * open)));
            RenderUtils.drawRoundedRect(ctx, x, y, w, h, R, Ui.bg());
            RenderUtils.drawRoundedRect(ctx, x, y, sbW + R, h, R, Ui.side());
            RenderUtils.drawRect(ctx, x + sbW, y, R, h, Ui.bg());
            RenderUtils.drawRect(ctx, x + sbW, y, 1, h, Ui.LINE);
            RenderUtils.drawRoundedOutline(ctx, x, y, w, h, R, 1, Ui.LINE_HI);

            renderSidebar(ctx, x, y, h, mx, my);

            float cx = contentX(), cw = contentW();
            renderHeader(ctx, cx, y, cw, mx, my);

            float viewTop = y + HEADER + 12, viewH = h - HEADER - 12 - 12;
            boolean popBlocks = overPopover(mx, my);
            renderContent(ctx, cx, viewTop, cw, viewH, popBlocks ? -10000 : mx, popBlocks ? -10000 : my, delta);

            renderSettings(ctx, mx, my);

            String showTip = tip;
            if (showTip == null && hoveredCard != null && System.currentTimeMillis() - hoverSince > 400) showTip = hoveredCard.module.getDisplayDescription();
            tipAnim.setTarget(showTip != null ? 1f : 0f);
            float ta = tipAnim.update();
            if (showTip != null) lastTip = showTip;
            if (ta > 0.05f && lastTip != null) Ui.tooltip(ctx, lastTip, mx, my, vw, ta);
        } finally {
            Component.setFlat(false);
        }
    }

    private void renderSidebar(GuiGraphicsExtractor ctx, float sx, float sy, float sh, int mx, int my) {
        float sw = sbW;
        int side = Ui.side();

        float ms = 22, mw = Logo.markWidth(ms), mkx = compact ? sx + sw / 2f - mw / 2f : sx + 23 - mw / 2f, mky = sy + 12;
        Logo.mark(ctx, mkx, mky, ms, Ui.accent());
        if (!compact) {
            Fonts.drawBold(ctx, ClientMod.NAME, sx + 40, sy + 14, Ui.text());
            Ui.label(ctx, "v" + ClientMod.VERSION, sx + 40, sy + 25, Ui.faint());
            Ui.label(ctx, I18n.tr("Модули"), sx + 16, sy + 48, Ui.faint());
            Ui.label(ctx, I18n.tr("Клиент"), sx + 16, sy + navTop(Category.COLORS) - 14, Ui.faint());
        } else {
            RenderUtils.drawRect(ctx, sx + 12, sy + navTop(Category.COLORS) - 6, sw - 24, 1, Ui.LINE);
        }

        float target = navTop(selected);
        if (!navSnapped) { navAnim.snap(target); navSnapped = true; }
        navAnim.setTarget(target);
        float sel = navAnim.update();
        float pa = Ui.anim("nav.sel", !searching());
        if (pa > 0.02f) {
            RenderUtils.drawRoundedRect(ctx, sx + 8, sy + sel, sw - 16, ITEM_H, Ui.RADIUS,
                    ColorUtils.withAlpha(ColorUtils.lerp(side, 0xFFFFFFFF, 0.08f), (int) (255 * pa)));
            RenderUtils.drawRoundedRect(ctx, sx + 8, sy + sel + 6, 2, ITEM_H - 12, 1, ColorUtils.multAlpha(Ui.accent(), pa));
        }

        for (Category c : NAV) {
            float iy = sy + navTop(c);
            boolean isSel = !searching() && c == selected;
            boolean hover = Ui.inside(mx, my, sx + 8, iy, sw - 16, ITEM_H);
            float ha = Ui.anim("nav." + c, hover && !isSel);
            if (ha > 0.02f) RenderUtils.drawRoundedRect(ctx, sx + 8, iy, sw - 16, ITEM_H, Ui.RADIUS,
                    ColorUtils.withAlpha(ColorUtils.lerp(side, 0xFFFFFFFF, 0.04f), (int) (255 * ha)));
            int fg = isSel ? Ui.text() : ColorUtils.lerp(Ui.dim(), Ui.text(), ha);
            int icon = isSel ? Ui.accent() : fg;
            if (!compact) {
                Icons.draw(ctx, c, sx + 22, iy + ITEM_H / 2f, icon, 0);
                Fonts.draw(ctx, c.getDisplayName(), sx + 34, iy + ITEM_H / 2f - 4, fg);
                String count = count(c);
                if (count != null) Fonts.draw(ctx, count, sx + sw - 16 - Fonts.width(count), iy + ITEM_H / 2f - 4, Ui.faint());
            } else {
                Icons.draw(ctx, c, sx + sw / 2f, iy + ITEM_H / 2f, icon, 0);
                if (hover) tip = c.getDisplayName();
            }
            hits.addSilent(sx + 8, iy, sw - 16, ITEM_H, () -> select(c));
        }

        AltManager am = AltManager.get();
        String name = Minecraft.getInstance().getUser().getName();
        boolean alt = am != null && am.altActive();
        if (!compact) {
            RenderUtils.drawRect(ctx, sx + 12, sy + sh - 46, sw - 24, 1, Ui.LINE);
            float px = sx + 8, pw = sw - 16, ph = 32, py = sy + sh - 40;
            boolean hover = Ui.inside(mx, my, px, py, pw, ph);
            float ha = Ui.anim("nav.profile", hover);
            if (ha > 0.02f) RenderUtils.drawRoundedRect(ctx, px, py, pw, ph, Ui.RADIUS,
                    ColorUtils.withAlpha(ColorUtils.lerp(side, 0xFFFFFFFF, 0.04f), (int) (255 * ha)));
            avatar(ctx, am, name, alt, px + 7, py + 7, 18);
            Fonts.draw(ctx, Fonts.trim(name, (int) (pw - 52)), px + 32, py + 6, Ui.text());
            Ui.label(ctx, I18n.tr(alt ? "оффлайн-альт" : "основной"), px + 32, py + 17, Ui.faint());
            Icons.draw(ctx, Icons.Glyph.SWAP, px + pw - 10, py + ph / 2f, ColorUtils.lerp(Ui.faint(), Ui.text(), ha));
            hits.addSilent(px, py, pw, ph, () -> select(Category.ACCOUNTS));
        } else {
            float s = 18, px = sx + sw / 2f - s / 2f, py = sy + sh - s - 12;
            if (Ui.inside(mx, my, px, py, s, s)) tip = name;
            avatar(ctx, am, name, alt, px, py, (int) s);
            hits.addSilent(px, py, s, s, () -> select(Category.ACCOUNTS));
        }
    }

    private static void avatar(GuiGraphicsExtractor ctx, AltManager am, String name, boolean alt, float x, float y, int size) {
        if (alt || am == null) Ui.letterAvatar(ctx, name, x, y, size);
        else Ui.face(ctx, Minecraft.getInstance().getGameProfile(), x, y, size);
    }

    private String count(Category c) {
        switch (c) {
            case COLORS -> { return null; }
            case CONFIGS -> { return String.valueOf(configs.configs().size()); }
            case ACCOUNTS -> {
                AltManager am = AltManager.get();
                return am == null || am.size() == 0 ? null : String.valueOf(am.size());
            }
            default -> {
                int on = 0;
                for (ModuleCard card : cards.get(c)) if (card.module.isToggled()) on++;
                return on == 0 ? null : String.valueOf(on);
            }
        }
    }

    private void renderHeader(GuiGraphicsExtractor ctx, float cx, float y, float cw, int mx, int my) {
        float bs = 20, by = y + 12, bx = cx + cw - bs;
        Ui.iconButton(ctx, null, "hdr.gear", Icons.Glyph.GEAR, bx, by, bs, settingsOpen, Ui.accent(), mx, my, null);
        hits.addSilent(bx, by, bs, bs, () -> {
            settingsOpen = !settingsOpen;
            search.focused = false;
            UiSounds.dropdown(settingsOpen);
        });
        if (Ui.inside(mx, my, bx, by, bs, bs) && !settingsOpen) tip = I18n.tr("Настройки меню");
        popX = bx + bs - POP_W;
        popY = by + bs + 6;
        if (inWorld()) {
            bx -= bs + 6;
            Ui.iconButton(ctx, hits, "hdr.hud", Icons.Glyph.LAYOUT, bx, by, bs, false, Ui.accent(), mx, my, screen::enterHudMode);
            if (Ui.inside(mx, my, bx, by, bs, bs)) tip = I18n.tr("Редактор HUD");
        }
        if (page() == null || search.focused) {
            float sw = Math.min(150, cw * 0.38f);
            bx -= sw + 6;
            search.render(ctx, bx, by, sw, bs, Icons.Glyph.SEARCH, mx, my);
        }

        String title = searching() ? I18n.tr("Поиск") : selected.getDisplayName();
        Fonts.drawScaled(ctx, title, cx, y + 15, 1.4f, Ui.text(), Fonts.BOLD);
        float tw = Fonts.width(title, Fonts.BOLD) * 1.4f;
        float maxSub = bx - 10 - (cx + tw + 8);
        if (maxSub > 30) Fonts.draw(ctx, Fonts.trim(subtitle(), (int) maxSub), cx + tw + 8, y + 18, Ui.faint());

        RenderUtils.drawRect(ctx, winX + sbW + 1, y + HEADER, winW - sbW - 1, 1, Ui.LINE);
    }

    private String subtitle() {
        if (searching()) return I18n.tr("найдено: %d", visibleCards().size());
        switch (selected) {
            case COLORS -> { return I18n.tr("цвета и шрифт"); }
            case CONFIGS -> { return I18n.tr("активный: %s", ClientMod.getInstance().getConfigManager().getCurrent()); }
            case ACCOUNTS -> {
                AltManager am = AltManager.get();
                return I18n.tr(am != null && am.altActive() ? "оффлайн-альт" : "аккаунт лаунчера");
            }
            default -> {
                List<ModuleCard> list = cards.get(selected);
                int on = 0;
                for (ModuleCard c : list) if (c.module.isToggled()) on++;
                return I18n.tr("%d из %d включено", on, list.size());
            }
        }
    }

    private void renderContent(GuiGraphicsExtractor ctx, float cx, float top, float cw, float viewH, int mx, int my, float delta) {
        Page page = page();
        if (page != null) {
            hoveredCard = null;
            page.render(ctx, cx, top, cw, viewH, mx, my);
            return;
        }

        List<ModuleCard> list = visibleCards();
        boolean single = list.size() <= 1 || cw < 320 || (!searching() && selected == Category.COLORS);
        int cols = single ? 1 : 2;
        float colW = (cw - GAP * (cols - 1)) / cols;
        float[] colY = new float[cols];
        float scroll = scrollAnim.update();
        for (int i = 0; i < list.size(); i++) {
            ModuleCard card = list.get(i);
            card.tickAnim();
            int col = cols == 1 ? 0 : i % 2;
            card.x = cx + col * (colW + GAP);
            card.y = top + colY[col] - scroll;
            card.w = colW;
            colY[col] += card.height() + GAP;
        }
        float contentHeight = 0;
        for (float v : colY) contentHeight = Math.max(contentHeight, v - GAP);
        float maxScroll = Math.max(0, contentHeight - viewH);
        scrollTarget = Mth.clamp(scrollTarget, 0, maxScroll);
        scrollAnim.setTarget(scrollTarget);

        boolean mouseInView = Ui.inside(mx, my, cx, top, cw, viewH);
        ctx.enableScissor((int) cx - 1, (int) top, (int) Math.ceil(cx + cw + 1), (int) Math.ceil(top + viewH));
        ModuleCard hovered = null;
        for (ModuleCard card : list) {
            if (card.y > top + viewH || card.y + card.height() < top) continue;
            card.render(ctx, mx, my, delta, mouseInView);
            if (mouseInView && Ui.inside(mx, my, card.x, card.y, card.w, ModuleCard.HEAD)) hovered = card;
        }
        if (list.isEmpty()) Fonts.drawCentered(ctx, I18n.tr("Ничего не найдено"), cx + cw / 2f, top + 40, Ui.faint(), Fonts.REGULAR);
        ctx.disableScissor();

        if (hovered != hoveredCard) {
            hoveredCard = hovered;
            hoverSince = System.currentTimeMillis();
        }
        if (hoveredCard != null && !hoveredCard.descriptionTrimmed()) hoveredCard = null;

        if (maxScroll > 0) {
            float barH = Math.max(20, viewH * viewH / (contentHeight + 1));
            float barY = top + (viewH - barH) * (scroll / maxScroll);
            RenderUtils.drawRoundedRect(ctx, cx + cw + 6, barY, 2, barH, 1, Ui.LINE_HI);
        }
    }

    private void renderSettings(GuiGraphicsExtractor ctx, int mx, int my) {
        ClickGui gui = ClickGui.get();
        if (gui != null && !gui.modern()) settingsOpen = false;
        float content = 0;
        for (Component c : uiSettings) if (c.isVisible()) content += c.getHeight() + 2;
        popH = 30 + content + 6;
        if (!settingsOpen) return;
        float x = popX, y = popY, w = POP_W, h = popH;
        RenderUtils.drawGlow(ctx, x, y, w, h, 6, 10, ColorUtils.withAlpha(0x000000, 130));
        RenderUtils.drawRoundedRect(ctx, x, y, w, h, 6, ColorUtils.lerp(Ui.bg(), 0xFFFFFFFF, 0.035f));
        RenderUtils.drawRoundedOutline(ctx, x, y, w, h, 6, 1, Ui.LINE_HI);
        Fonts.drawBold(ctx, I18n.tr("Интерфейс"), x + 10, y + 10, Ui.text());
        RenderUtils.drawRect(ctx, x + 10, y + 24, w - 20, 1, Ui.LINE);
        boolean inside = Ui.inside(mx, my, x, y, w, h);
        int cmx = inside ? mx : -10000, cmy = inside ? my : -10000;
        float yy = y + 29;
        for (Component c : uiSettings) {
            if (!c.isVisible()) continue;
            c.setBounds(x + 6, yy, w - 12);
            c.render(ctx, cmx, cmy, 0);
            yy += c.getHeight() + 2;
        }
    }

    boolean mouseClicked(double mx, double my, int button) {
        if (settingsOpen) {
            if (overPopover(mx, my)) {
                for (Component c : uiSettings) if (c.isVisible() && c.mouseClicked(mx, my, button)) return true;
                return true;
            }
            settingsOpen = false;
            UiSounds.dropdown(false);
            return true;
        }
        if (!overWindow(mx, my)) {
            search.focused = false;
            ModuleCard.stopListening();
            return false;
        }
        float cx = contentX(), cw = contentW();
        float viewTop = renderY + HEADER + 12, viewH = winH - HEADER - 12 - 12;
        boolean inView = Ui.inside(mx, my, cx - 1, viewTop, cw + 2, viewH);
        if (!inView) ModuleCard.stopListening();

        if (search.mouseClicked(mx, my, button)) return true;
        Page page = page();
        if (page != null && page.mouseClicked(mx, my, button)) return true;
        if (button == 0 && hits.click(mx, my)) return true;

        if (page == null && inView) {
            for (ModuleCard card : visibleCards()) {
                if (card.mouseClicked(mx, my, button)) return true;
            }
        }
        boolean onHeader = my <= renderY + HEADER && mx >= cx - 12;
        boolean onBrand = mx <= winX + sbW && my <= renderY + 40;
        if (button == 0 && (onHeader || onBrand)) {
            dragging = true;
            dragX = (float) (mx - winX);
            dragY = (float) (my - winY);
        }
        return true;
    }

    void mouseReleased(double mx, double my, int button) {
        for (ModuleCard c : allCards) c.mouseReleased(mx, my, button);
        for (Component c : uiSettings) c.mouseReleased(mx, my, button);
        dragging = false;
    }

    void scroll(double mx, double my, double vertical) {
        if (overPopover(mx, my)) return;
        Page page = page();
        if (page != null) page.scroll(vertical);
        else scrollTarget -= (float) vertical * 26f;
    }

    boolean keyPressed(int key, int scanCode, int modifiers, boolean ctrl) {
        if (settingsOpen) {
            for (Component c : uiSettings) if (c.isVisible() && c.keyPressed(key, scanCode, modifiers)) return true;
            if (key == InputConstants.KEY_ESCAPE) { settingsOpen = false; UiSounds.dropdown(false); return true; }
        }
        if (search.keyPressed(key, ctrl)) return true;
        Page page = page();
        if (page != null && page.keyPressed(key, ctrl)) return true;
        if (page == null) for (ModuleCard c : visibleCards()) if (c.keyPressed(key, scanCode, modifiers)) return true;
        if (ctrl && key == InputConstants.KEY_F) {
            search.focus();
            settingsOpen = false;
            return true;
        }
        return false;
    }

    boolean charTyped(char chr, int modifiers) {
        if (search.charTyped(chr)) return true;
        Page page = page();
        if (page != null && page.charTyped(chr)) return true;
        if (settingsOpen) for (Component c : uiSettings) if (c.isVisible() && c.charTyped(chr, modifiers)) return true;
        if (page == null) for (ModuleCard c : visibleCards()) if (c.charTyped(chr, modifiers)) return true;
        return false;
    }

    boolean typing() {
        if (search.focused) return true;
        Page page = page();
        if (page != null && page.typing()) return true;
        for (Component c : uiSettings) if (c.isTyping()) return true;
        if (page == null) for (ModuleCard c : visibleCards()) if (c.isTyping()) return true;
        return false;
    }

    void onClose() {
        dragging = false;
        search.focused = false;
        settingsOpen = false;
        ModuleCard.stopListening();
    }
}
