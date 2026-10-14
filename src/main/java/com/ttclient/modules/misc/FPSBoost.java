package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;

public class FPSBoost extends Module {
    public final BoolSetting unfocusedCPU = addSetting(new BoolSetting("Unfocused CPU", "Drop FPS cap when the window is unfocused", true));
    public final NumberSetting unfocusedLimit = addSetting(new NumberSetting("Unfocused FPS", "FPS cap while unfocused", 10, 5, 60, 1));
    public final BoolSetting particleLimit = addSetting(new BoolSetting("Particle Limit note", "Reminder only — vanilla particles are not patched yet", false));

    private int savedLimit = -1;

    public FPSBoost() {
        super("FPSBoost", "Lower FPS when unfocused to save CPU", Category.MISC);
        // off by default — avoid touching options during early load / unwanted config restore
    }

    @Override
    public void onTick() {
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.options == null) return;
        if (!unfocusedCPU.get()) {
            restore();
            return;
        }
        boolean focused = minecraft.isWindowActive();
        if (!focused) {
            if (savedLimit < 0) {
                try {
                    savedLimit = minecraft.options.framerateLimit().get();
                } catch (Throwable t) {
                    return;
                }
            }
            try {
                minecraft.options.framerateLimit().set(unfocusedLimit.getInt());
            } catch (Throwable ignored) {}
        } else {
            restore();
        }
    }

    @Override
    public void onDisable() {
        restore();
    }

    private void restore() {
        Minecraft minecraft = mc();
        if (savedLimit >= 0 && minecraft != null && minecraft.options != null) {
            try {
                minecraft.options.framerateLimit().set(savedLimit);
            } catch (Throwable ignored) {}
            savedLimit = -1;
        }
    }
}
