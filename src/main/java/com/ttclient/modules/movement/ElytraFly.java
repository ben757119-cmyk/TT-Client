package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public class ElytraFly extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Boost speed", 1.8, 0.5, 4.0, 0.1));

    public ElytraFly() {
        super("ElytraFly", "Boost while gliding with elytra", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null) return;
        LocalPlayer p = mc.player;
        if (!p.isFallFlying()) return;
        Vec3 look = p.getLookAngle();
        double s = speed.get() * 0.15;
        if (mc.options.keyUp.isDown()) {
            p.setDeltaMovement(p.getDeltaMovement().add(look.scale(s)));
        }
        if (mc.options.keyJump.isDown()) {
            p.setDeltaMovement(p.getDeltaMovement().add(0, s, 0));
        }
    }
}
