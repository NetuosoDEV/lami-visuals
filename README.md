# LamiVisuals

Minecraft 26.3 visual client-side mod (Fabric). Hits/kills effects, kill streak banners, themed hud, procedural skies, postprocessing, free look, only-you-see cosmetics. Title, pause and death screens.

It's a visual mod, no packets are sent, game mechanics not affected. It only changes what you see on your screen.

Requirements:
 * Minecraft 26.3
 * Fabric Loader 0.19.5+
 * Fabric API 0.162.0+26.3+
 * Java 25

To install:

1. Install Fabric Loader for Minecraft 26.3.
2. Place Fabric API, LamiVisuals jar in .minecraft/mods
3. Run game, open menu with <kbd>Right Shift</kbd>

Building:
 * JDK 25
 * Gradle will download Minecraft, Fabric API on first run.
   
bash
 ./gradlew build

 * Jar will be in build/libs/. To run development client:
   
bash
 ./gradlew runClient

 * -PextraResources=<dir> will add another resource dir (own assets/... tree) to jar. It's for your local assets not present in this repo.

Modules:

| Category | Modules |
|---|---|
| Combat | TargetHUD, TargetESP, HitParticles, DamageNumbers, HitCounter, KillEffect, HitMarker, HitSound, HitFX, KillStreak, TotemPop |
| Render | Trajectories, JumpCircles, ParticleRain, ChinaHat, Trails, BlockOverlay, Crosshair, ViewModel, SwingAnimations, Camera, Zoom, FreeLook, NoRender, Hitboxes, Ambience, Cosmetics, GlintColor, Shaders, ChunkAnimator |
| HUD | Watermark, ArrayList, Notifications, Potions, ArmorHUD, Cooldowns, InventoryHUD, LowHP, FoodHUD, Keystrokes, SessionStats, ShulkerPreview, ChatTab |
| Utility | ClickGUI, MainMenu, FakePlayer, Screens |
| Colors | Theme |

All HUD elements are movable and resizable in HUD editor.

Project layout:

bash
 src/main/java/ru/client/
 core/    entry point, configs, accounts, translations, session stats
 modules/    module/settings base classes. Module registry.
 modules/impl/    modules by category: combat, render, hud, utility
 mixins/    mixins. Listed in lamivisuals.mixins.json
 ui/    ClickGUI (Modern, Legacy). Main menu. Pause and death screens. Chat. Theme.
 utils/    rendering helpers. Postprocessing. Particles. Models. Animation

src/main/resources/assets/
 lamivisuals/    textures, sounds, fonts, shaders, wing models, translations
 minecraft/    terrain shader override. Used by ChunkAnimator.

Configuration.
In .minecraft/config/lamivisuals/. One json file for config. active.txt: current config name. alts.json: offline accounts.
Configs are managed in menu: Configs tab.

Translations.
Interface: English, Russian, Spanish. Russian text - in code. English, Spanish - in assets/lamivisuals/i18n/en.json, es.json. by russian source string. desc - module descriptions. ui - ui text.
Module, setting, option names - English (also used as config keys).

Compatibility:
 * ChunkAnimator uses vanilla terrain shader. Sodium/Iris, resource pack which replaces terrain.vsh will have no effect.
 * Shaders, HitFX, sky shaders, chat blur. Use custom glsl. If driver can't compile one of them, it will be disabled. Log: warning.

License: GNU General Public License v3.0. See LICENSE.

Third-party assets.

 * Inter typeface. By Rasmus Andersson. [Inter](https://rsms.me/inter/) SIL Open Font License 1.1. assets/lamivisuals/font/Inter-OFL.txt
 * Bonk Sound effect. By thecoolcookie17. [Pixabay Content License](https://pixabay.com/service/license-summary/)
