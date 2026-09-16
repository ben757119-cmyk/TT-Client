package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;

public class Fullbright extends Module {
    public final NumberSetting gamma = addSetting(new NumberSetting("Gamma", "Brightness gamma", 16.0, 1.0, 16.0, 0.5));
    private double oldGamma = 1.0;

    public Fullbright() {
        super("Fullbright", "Brighten the world", Category.RENDER);
    }

    @Override
    public void onEnable() {
        Minecraft mc = mc();
        if (mc == null || mc.options == null) return;
        try {
            oldGamma = mc.options.gamma().get();
            mc.options.gamma().set(gamma.get());
        } catch (Exception ignored) {}
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.options == null) return;
        try {
            if (mc.options.gamma().get() != gamma.get()) {
                mc.options.gamma().set(gamma.get());
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onDisable() {
        Minecraft mc = mc();
        if (mc == null || mc.options == null) return;
        try {
            mc.options.gamma().set(oldGamma);
        } catch (Exception ignored) {}
    }
}
