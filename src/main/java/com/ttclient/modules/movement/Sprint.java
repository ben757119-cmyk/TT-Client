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
        if (mc.player.input.getMoveVector().y > 0 || (omni.get() && mc.player.input.getMoveVector().lengthSquared() > 0)) {
            if (!mc.player.isShiftKeyDown() && !mc.player.isUsingItem()) {
                mc.player.setSprinting(true);
            }
        }
    }
}
