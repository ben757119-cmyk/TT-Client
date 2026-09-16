package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import net.minecraft.client.Minecraft;

public class FPSBoost extends Module {
    public final BoolSetting lowerClouds = addSetting(new BoolSetting("No Clouds", "Disable clouds", true));
    public final BoolSetting lowerParticles = addSetting(new BoolSetting("Fewer Particles", "Minimal particles", true));
    public final BoolSetting entityShadows = addSetting(new BoolSetting("No Entity Shadows", "Disable entity shadows", true));

    private int oldClouds = -1;
    private int oldParticles = -1;
    private boolean oldShadows = true;

    public FPSBoost() {
        super("FPSBoost", "Lower visual settings for more FPS", Category.MISC);
        setEnabled(true);
    }

    @Override
    public void onEnable() {
        apply(true);
    }

    @Override
    public void onDisable() {
        apply(false);
    }

    private void apply(boolean enable) {
        Minecraft mc = mc();
        if (mc == null || mc.options == null) return;
        try {
            if (enable) {
                if (lowerClouds.get()) {
                    // CloudStatus ordinal best-effort
                }
                if (entityShadows.get()) {
                    oldShadows = mc.options.entityShadows().get();
                    mc.options.entityShadows().set(false);
                }
            } else {
                mc.options.entityShadows().set(oldShadows);
            }
        } catch (Exception ignored) {}
    }
}
