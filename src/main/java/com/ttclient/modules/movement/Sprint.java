package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import net.minecraft.client.Minecraft;

public class Sprint extends Module {
    public final BoolSetting omni = addSetting(new BoolSetting("Omni", "Sprint in any direction", false));

    public Sprint() {
        super("Sprint", "Always sprint", Category.MOVEMENT);
        setEnabled(true);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null) return;
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
