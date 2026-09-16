package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class Step extends Module {
    public final NumberSetting height = addSetting(new NumberSetting("Height", "Step height", 1.0, 0.6, 2.5, 0.1));
    private double old = 0.6;

    public Step() {
        super("Step", "Step up full blocks", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        var inst = mc.player.getAttribute(Attributes.STEP_HEIGHT);
        if (inst != null) {
            old = inst.getBaseValue();
            inst.setBaseValue(height.get());
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        var inst = mc.player.getAttribute(Attributes.STEP_HEIGHT);
        if (inst != null) inst.setBaseValue(height.get());
    }

    @Override
    public void onDisable() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        var inst = mc.player.getAttribute(Attributes.STEP_HEIGHT);
        if (inst != null) inst.setBaseValue(old);
    }
}
