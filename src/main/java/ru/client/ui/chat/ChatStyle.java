package ru.client.ui.chat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import ru.client.modules.impl.hud.ChatTab;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.RenderUtils;
import ru.client.utils.render.UiBlur;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public final class ChatStyle implements ChatComponent.ChatGraphicsAccess {

    public static GuiGraphicsExtractor graphics;
    public static int ticks;
    public static boolean drawing;

    private static final Map<GuiMessage, Long> FIRST_SEEN = new WeakHashMap<>();

    private final ChatComponent.ChatGraphicsAccess d;
    private final Map<FormattedCharSequence, GuiMessage.Line> lines = new IdentityHashMap<>();
    private final List<int[]> pending = new ArrayList<>();
    private boolean flushed;

    public ChatStyle(ChatComponent.ChatGraphicsAccess delegate, List<GuiMessage.Line> trimmed) {
        this.d = delegate;
        for (GuiMessage.Line l : trimmed) lines.put(l.content(), l);
    }

    @Override
    public void updatePose(Consumer<Matrix3x2f> updater) { d.updatePose(updater); }

    @Override
    public void fill(int x0, int y0, int x1, int y1, int color) {
        if (!flushed && x0 == -4 && ChatTab.roundedChat()) {
            pending.add(new int[]{x0, y0, x1, y1, ColorUtils.alpha(color)});
            return;
        }
        flush();
        d.fill(x0, y0, x1, y1, color);
    }

    private void flush() {
        if (flushed) return;
        flushed = true;
        if (pending.isEmpty() || graphics == null) return;
        int panel = Theme.get().panel(), accent = Theme.get().accentSolid();
        int i = 0;
        while (i < pending.size()) {
            int[] first = pending.get(i);
            int j = i;
            while (j + 1 < pending.size() && Math.abs(pending.get(j + 1)[4] - first[4]) <= 2 && pending.get(j + 1)[1] == pending.get(j)[3]) j++;
            int[] last = pending.get(j);
            int a = first[4];
            float x = first[0], y = first[1], w = first[2] - first[0], h = last[3] - first[1];
            boolean blur = ChatTab.blur() && UiBlur.available();
            if (blur) UiBlur.panel(graphics, x, y, w, h, 3, Math.min(255, a * 2), ChatTab.blurStrength());
            RenderUtils.drawRoundedRect(graphics, x, y, w, h, 3, ColorUtils.withAlpha(panel, blur ? a * 3 / 5 : a));
            if (ChatTab.accentBar()) RenderUtils.drawRoundedRect(graphics, x, y + 1, 1.5f, h - 2, 0.75f, ColorUtils.withAlpha(accent, Math.min(255, a * 2)));
            i = j + 1;
        }
    }

    private float appear(FormattedCharSequence message) {
        if (!ChatTab.slideIn()) return 1f;
        GuiMessage.Line line = lines.get(message);
        if (line == null || ticks - line.addedTime() > 40) return 1f;
        long now = System.currentTimeMillis();
        long first = FIRST_SEEN.computeIfAbsent(line.parent(), k -> now);
        float t = Math.min(1f, (now - first) / 220f);
        float u = 1f - t;
        return 1f - u * u * u;
    }

    @Override
    public boolean handleMessage(int textTop, float opacity, FormattedCharSequence message) {
        flush();
        float k = appear(message);
        if (k >= 0.999f) return d.handleMessage(textTop, opacity, message);
        float dx = -(1f - k) * 16f;
        d.updatePose(p -> p.translate(dx, 0));
        boolean r = d.handleMessage(textTop, opacity * k, message);
        d.updatePose(p -> p.translate(-dx, 0));
        return r;
    }

    @Override
    public void handleTag(int x0, int y0, int x1, int y1, float opacity, GuiMessageTag tag) {
        flush();
        d.handleTag(x0, y0, x1, y1, opacity, tag);
    }

    @Override
    public void handleTagIcon(int left, int bottom, boolean forceVisible, GuiMessageTag tag, GuiMessageTag.Icon icon) {
        flush();
        d.handleTagIcon(left, bottom, forceVisible, tag, icon);
    }
}
