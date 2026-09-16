package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public class LongJump extends Module {
    public final NumberSetting boost = addSetting(new NumberSetting("Boost", "Horizontal boost", 1.8, 1.0, 3.5, 0.1));
    private boolean boosted;

    public LongJump() {
        super("LongJump", "Boost horizontal speed on jump", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        LocalPlayer p = mc.player;
        if (p.onGround()) {
            boosted = false;
            return;
        }
        if (!boosted && p.getDeltaMovement().y > 0) {
            Vec3 look = p.getLookAngle();
            double s = boost.get() * 0.35;
            p.setDeltaMovement(look.x * s, p.getDeltaMovement().y, look.z * s);
            boosted = true;
        }
    }
}
