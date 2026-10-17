package ru.client.modules.impl.hud;

import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.Modules;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.NumberSetting;

public final class ChatTab extends Module {

    private final BooleanSetting roundedChat = register(new BooleanSetting("Rounded Chat", true));
    private final BooleanSetting slideIn = register(new BooleanSetting("Slide In", true));
    private final BooleanSetting accentBar = register(new BooleanSetting("Accent Bar", true));
    private final BooleanSetting chatInput = register(new BooleanSetting("Styled Input", true));
    private final BooleanSetting styledTab = register(new BooleanSetting("Styled Tab", true));
    private final BooleanSetting tabPing = register(new BooleanSetting("Ping Numbers", true));
    private final BooleanSetting blur = register(new BooleanSetting("Blur", true));
    private final NumberSetting blurStrength = register(new NumberSetting("Blur Strength", 1.0, 0.5, 2.5, 0.1));

    public ChatTab() {
        super("ChatTab", "Чат и Tab-список в стиле LamiVisuals", Category.HUD);
        accentBar.visibleWhen(roundedChat::isEnabled);
        blurStrength.visibleWhen(blur::isEnabled);
    }

    private static ChatTab on() { return Modules.enabled(ChatTab.class); }

    public static boolean roundedChat() { ChatTab m = on(); return m != null && m.roundedChat.isEnabled(); }
    public static boolean slideIn() { ChatTab m = on(); return m != null && m.slideIn.isEnabled(); }
    public static boolean accentBar() { ChatTab m = on(); return m != null && m.accentBar.isEnabled(); }
    public static boolean chatInput() { ChatTab m = on(); return m != null && m.chatInput.isEnabled(); }
    public static boolean styledTab() { ChatTab m = on(); return m != null && m.styledTab.isEnabled(); }
    public static boolean tabPing() { ChatTab m = on(); return m != null && m.tabPing.isEnabled(); }
    public static boolean blur() { ChatTab m = on(); return m != null && m.blur.isEnabled(); }
    public static float blurStrength() { ChatTab m = on(); return m == null ? 1f : m.blurStrength.getFloat(); }
}
