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
        if (mc.options != null) oldGamma = mc.options.gamma().get();
    }

    @Override
    public void onDisable() {
        if (mc.options != null && oldGamma >= 0) mc.options.gamma().set(oldGamma);
    }

    @Override
    public void onTick() {
        if (mc.options != null && mode.get().equals("Gamma")) {
            mc.options.gamma().set(gamma.get());
        }
    }
}
