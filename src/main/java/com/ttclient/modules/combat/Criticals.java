package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class Criticals extends Module {
    public Criticals() {
        super("Criticals", "Jump slightly before attacking for crits", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null) return;
        if (!mc.options.keyAttack.isDown()) return;
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.ENTITY) return;
        if (!(mc.hitResult instanceof EntityHitResult)) return;
        if (mc.player.onGround()) {
            mc.player.jumpFromGround();
        }
    }
}
