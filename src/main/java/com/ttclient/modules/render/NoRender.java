package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;

public class NoRender extends Module {
    public final BoolSetting fire = addSetting(new BoolSetting("Fire", "Hide fire overlay", true));
    public final BoolSetting hurtcam = addSetting(new BoolSetting("HurtCam", "Disable hurt camera", true));
    public final BoolSetting bossbar = addSetting(new BoolSetting("BossBar", "Hide boss bars", false));
    public final BoolSetting scoreboard = addSetting(new BoolSetting("Scoreboard", "Hide scoreboard", false));
    public final BoolSetting potions = addSetting(new BoolSetting("Potions", "Hide potion icons", false));
    public final BoolSetting weather = addSetting(new BoolSetting("Weather", "Disable rain/snow", false));
    public final BoolSetting explosions = addSetting(new BoolSetting("Explosions", "Reduce explosion particles", true));
    public final BoolSetting particles = addSetting(new BoolSetting("Particles", "Limit particles", false));

    public NoRender() {
        super("NoRender", "Disable various render elements for FPS", Category.RENDER);
    }
}
