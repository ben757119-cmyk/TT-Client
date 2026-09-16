package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class BoatFly extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Vehicle fly speed", 1.2, 0.3, 3.0, 0.1));

    public BoatFly() {
        super("BoatFly", "Fly while riding a vehicle", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null) return;
        Entity vehicle = mc.player.getVehicle();
        if (vehicle == null) return;
        Vec3 look = mc.player.getLookAngle();
        double s = speed.get() * 0.4;
        double mx = 0, my = 0, mz = 0;
        if (mc.options.keyUp.isDown()) {
            mx += look.x * s;
            mz += look.z * s;
        }
        if (mc.options.keyJump.isDown()) my += s;
        if (mc.options.keyShift.isDown()) my -= s;
        vehicle.setDeltaMovement(mx, my, mz);
    }
}
