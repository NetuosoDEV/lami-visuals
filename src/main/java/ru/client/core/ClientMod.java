package ru.client.core;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import ru.client.modules.impl.utility.MainMenu;
import ru.client.ui.menu.LamiMainMenu;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import ru.client.utils.render.CosmeticWings;
import ru.client.utils.render.Render3D;
import ru.client.utils.render.SkyShader;
import ru.client.utils.render.ScreenFx;
import ru.client.utils.render.PostFx;
import ru.client.utils.render.UiBlur;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.client.core.alt.AltManager;
import ru.client.core.config.ConfigManager;
import ru.client.modules.ModuleManager;
import ru.client.modules.Modules;
import ru.client.modules.impl.render.BlockOverlay;

public final class ClientMod implements ClientModInitializer {

    public static final String MOD_ID = "lamivisuals";
    public static final String NAME = "LamiVisuals";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String VERSION = FabricLoader.getInstance().getModContainer(MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");

    private static ClientMod instance;

    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private AltManager altManager;

    @Override
    public void onInitializeClient() {
        instance = this;
        I18n.load();
        Render3D.init();
        SkyShader.init();
        ScreenFx.init();
        UiBlur.init();
        PostFx.init();

        moduleManager = new ModuleManager();
        moduleManager.init();

        configManager = new ConfigManager(moduleManager);
        configManager.load();

        altManager = new AltManager();
        altManager.load();
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> altManager.onClientStarted());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            moduleManager.onTick();
            CombatTracker.tick(moduleManager);
        });

        LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> {
            Camera cam = ctx.gameRenderer().mainCamera();
            PoseStack matrices = ctx.poseStack();
            Render3D.beginFrame(ctx.submitNodeCollector(), cam);
            try {
                moduleManager.onRender3D(matrices, cam, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true));
            } finally {
                Render3D.endFrame();
            }
        });

        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
            if (renderer instanceof AvatarRenderer<?> avatar) helper.register(new CosmeticWings.Layer(avatar));
        });

        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((ctx, outline) -> Modules.enabled(BlockOverlay.class) == null);

        HudElementRegistry.attachElementBefore(VanillaHudElements.DEMO_TIMER, Identifier.fromNamespaceAndPath(MOD_ID, "hud"),
                (ctx, deltaTracker) -> {
                    if (Minecraft.getInstance().gui.hud.isHidden()) return;
                    moduleManager.onRender2D(ctx, deltaTracker.getGameTimeDeltaPartialTick(true));
                });

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen.getClass() != TitleScreen.class) return;
            Screens.getWidgets(screen).add(Button.builder(Component.literal("LamiVisuals"), b -> {
                MainMenu menu = moduleManager.get(MainMenu.class);
                if (menu != null) menu.setToggled(true);
                configManager.save();
                client.gui.setScreen(new LamiMainMenu());
            }).bounds(6, 6, 90, 20).build());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CombatTracker.reset());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> configManager.save());

        LOGGER.info("{} {} initialized: {} modules", NAME, VERSION, moduleManager.getModules().size());
    }

    public static ClientMod getInstance() { return instance; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public AltManager getAltManager() { return altManager; }
}
