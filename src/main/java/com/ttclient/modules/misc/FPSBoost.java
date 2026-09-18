package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;

public class FPSBoost extends Module {
    public final BoolSetting unfocusedCPU = addSetting(new BoolSetting("Unfocused CPU", "Drop FPS cap when the window is unfocused", true));
    public final NumberSetting unfocusedLimit = addSetting(new NumberSetting("Unfocused FPS", "FPS cap while unfocused", 10, 5, 60, 1));
    public final BoolSetting particleLimit = addSetting(new BoolSetting("Particle Limit note", "Reminder only — vanilla particles are not patched yet", false));

    private int savedLimit = -1;

    public FPSBoost() {
        super("FPSBoost", "Lower FPS when unfocused to save CPU", Category.MISC);
        setEnabled(true);
    }

    @Override
    public void onTick() {
        if (mc.options == null) return;
        if (!unfocusedCPU.get()) {
            restore();
            return;
        }
        boolean focused = mc.isWindowActive();
        if (!focused) {
            if (savedLimit < 0) {
                savedLimit = mc.options.framerateLimit().get();
            }
            mc.options.framerateLimit().set(unfocusedLimit.getInt());
        } else {
            restore();
        }
    }

    @Override
    public void onDisable() {
        restore();
    }

    private void restore() {
        if (savedLimit >= 0 && mc.options != null) {
            mc.options.framerateLimit().set(savedLimit);
            savedLimit = -1;
        }
    }
}
