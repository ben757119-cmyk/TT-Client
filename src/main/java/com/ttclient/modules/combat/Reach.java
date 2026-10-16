package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Mild reach boost via interaction-range attributes. */
public class Reach extends Module {
    public final NumberSetting reach = addSetting(new NumberSetting("Reach", "Attack reach", 4.5, 3.0, 6.0, 0.1));

    private double oldEntity = 3.0;
    private double oldBlock = 4.5;

    public Reach() {
        super("Reach", "Extend attack / block reach", Category.COMBAT);
    }

    @Override
    public void onEnable() { apply(); }

    @Override
    public void onDisable() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        try {
            AttributeInstance e = mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
            if (e != null) e.setBaseValue(oldEntity);
        } catch (Throwable ignored) {}
        try {
            AttributeInstance b = mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
            if (b != null) b.setBaseValue(oldBlock);
        } catch (Throwable ignored) {}
    }

    @Override
    public void onTick() { apply(); }

    private void apply() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        double r = reach.get();
        try {
            AttributeInstance e = mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
            if (e != null) {
                if (e.getBaseValue() < 10) oldEntity = e.getBaseValue();
                e.setBaseValue(r);
            }
        } catch (Throwable ignored) {}
        try {
            AttributeInstance b = mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
            if (b != null) {
                if (b.getBaseValue() < 10) oldBlock = b.getBaseValue();
                b.setBaseValue(r);
            }
        } catch (Throwable ignored) {}
    }
}
