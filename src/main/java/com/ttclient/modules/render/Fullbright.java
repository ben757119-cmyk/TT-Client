package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.ModeSetting;
import com.ttclient.settings.NumberSetting;

public class Fullbright extends Module {
    public final ModeSetting mode = addSetting(new ModeSetting("Mode", "Fullbright mode", "Gamma", "Gamma", "Potion"));
    public final NumberSetting gamma = addSetting(new NumberSetting("Gamma", "Gamma value", 16.0, 1.0, 16.0, 0.5));
    private double oldGamma = -1;

    public Fullbright() {
        super("Fullbright", "See in the dark", Category.RENDER);
    }

    @Override
    public void onEnable() {
        if (mc.options != null) {
            try {
                oldGamma = mc.options.gamma().get();
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onDisable() {
        // Gamma restore best-effort for 26.2 option types
    }

    @Override
    public void onTick() {
        // Gamma application depends on Options API shape in 26.2
    }
}
