package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class TargetInfo extends Module {
    public TargetInfo() {
        super("TargetInfo", "Show the entity you are looking at", Category.RENDER);
        setEnabled(true);
    }

    @Override
    public void onRender2D(GuiGraphics g, float partialTick) {
        if (mc.player == null || mc.hitResult == null || mc.options.hideGui) return;
        if (mc.hitResult.getType() != HitResult.Type.ENTITY) return;
        Entity e = ((EntityHitResult) mc.hitResult).getEntity();
        String name = e.getName().getString();
        String extra = "";
        if (e instanceof LivingEntity living) {
            extra = String.format(" §7%.0f/%.0f", living.getHealth(), living.getMaxHealth());
        }
        String text = "Look §f" + name + extra;
        int w = mc.font.width(text);
        int x = (mc.getWindow().getGuiScaledWidth() - w) / 2;
        g.drawString(mc.font, text, x, mc.getWindow().getGuiScaledHeight() / 2 + 16, 0xFF00E8A0, false);
    }
}
