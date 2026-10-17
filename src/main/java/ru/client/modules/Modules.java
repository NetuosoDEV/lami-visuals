package ru.client.modules;

import ru.client.core.ClientMod;

public final class Modules {
    private Modules() {}

    public static <T extends Module> T get(Class<T> clazz) {
        ClientMod mod = ClientMod.getInstance();
        return mod == null || mod.getModuleManager() == null ? null : mod.getModuleManager().get(clazz);
    }

    public static <T extends Module> T enabled(Class<T> clazz) {
        T m = get(clazz);
        return m != null && m.isToggled() ? m : null;
    }
}
