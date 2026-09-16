package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;

public class WTap extends Module {
    private int resetTicks;

    public WTap() {
        super("WTap", "Briefly stop forward movement when attacking", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null) return;
        if (resetTicks > 0) {
            mc.options.keyUp.setDown(false);
            resetTicks--;
            return;
        }
        if (mc.options.keyAttack.isDown()
                && mc.hitResult != null
                && mc.hitResult.getType() == HitResult.Type.ENTITY) {
            resetTicks = 2;
        }
    }
}
