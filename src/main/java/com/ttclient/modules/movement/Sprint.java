package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;

public class Sprint extends Module {
    public final BoolSetting omni = addSetting(new BoolSetting("Omni", "Sprint in all directions", false));
    public final BoolSetting keep = addSetting(new BoolSetting("Keep", "Keep sprint after stopping", true));

    public Sprint() {
        super("Sprint", "Automatically sprint", Category.MOVEMENT);
        setEnabled(true);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        boolean forward = mc.options.keyUp.isDown();
        boolean any = forward
                || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown()
                || mc.options.keyRight.isDown();
        if (forward || (omni.get() && any)) {
            if (!mc.player.isShiftKeyDown() && mc.player.getFoodData().getFoodLevel() > 6) {
                mc.player.setSprinting(true);
            }
        }
    }
}
