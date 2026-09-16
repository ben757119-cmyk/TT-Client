package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;

public class AutoSprintReset extends Module {
    private int cooldown;

    public AutoSprintReset() {
        super("AutoSprintReset", "Reset sprint after attacking for better combos", Category.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null) return;
        if (cooldown > 0) {
            mc.player.setSprinting(false);
            cooldown--;
            return;
        }
        if (mc.options.keyAttack.isDown()
                && mc.hitResult != null
                && mc.hitResult.getType() == HitResult.Type.ENTITY) {
            cooldown = 3;
        } else if (mc.options.keyUp.isDown()) {
            mc.player.setSprinting(true);
        }
    }
}
