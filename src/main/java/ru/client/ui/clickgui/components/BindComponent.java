package ru.client.ui.clickgui.components;

import ru.client.core.I18n;
import ru.client.ui.UiSounds;

import ru.client.utils.render.Fonts;
import com.mojang.blaze3d.platform.InputConstants;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.RenderUtils;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class BindComponent extends Component {

    private final java.util.function.Supplier<String> label;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private boolean listening;

    public BindComponent(java.util.function.Supplier<String> label, IntSupplier getter, IntConsumer setter) {
        this.label = label;
        this.getter = getter;
        this.setter = setter;
    }

    @Override public float getHeight() { return 14; }

    @Override
    public void render(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        Theme t = Theme.get();
        String value = listening ? "..." : keyName(getter.getAsInt());
        float w = Fonts.width(value) + 8;
        label(ctx, label.get(), x + 4, x + width - 4 - w, y + 3, t.textDim());
        float bx = x + width - 4 - w;
        RenderUtils.drawRoundedRect(ctx, bx, y + 2, w, 10, 3,
                listening ? ColorUtils.withAlpha(accent(), 140) : 0xFF2A2A33);
        text(ctx, value, bx + 4, y + 3, t.text());
    }

    public static String keyName(int key) {
        if (key == -1) return I18n.tr("Нет");
        return InputConstants.Type.KEYBOARD.getOrCreate(key).getDisplayName().getString();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && inside(mx, my, x, y, width, getHeight())) {
            UiSounds.click();
            listening = !listening;
            return true;
        }
        listening = false;
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!listening) return false;
        if (key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_DELETE || key == InputConstants.KEY_BACKSPACE) {
            setter.accept(-1);
        } else {
            setter.accept(key);
        }
        listening = false;
        return true;
    }
}
