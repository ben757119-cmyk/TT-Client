package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Soft velocity reduction: scales horizontal knockback each tick while airborne after hit.
 * Full packet cancel needs mixins; this is a client-side mitigation.
 */
public class Velocity extends Module {
    public final NumberSetting horizontal = addSetting(new NumberSetting("Horizontal", "Keep % of XZ velocity", 0.0, 0.0, 1.0, 0.05));
    public final NumberSetting vertical = addSetting(new NumberSetting("Vertical", "Keep % of Y velocity", 1.0, 0.0, 1.0, 0.05));

    public Velocity() {
        super("Velocity", "Reduce knockback (client)", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        if (mc.player.hurtTime <= 0) return;
        Vec3 v = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(
                v.x * horizontal.get(),
                v.y * vertical.get(),
                v.z * horizontal.get()
        );
    }
}
