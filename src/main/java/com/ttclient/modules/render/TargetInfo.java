package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class TargetInfo extends Module {
    public TargetInfo() {
        super("TargetInfo", "Show looked-at entity info", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphics graphics, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.hitResult == null) return;
        if (mc.hitResult.getType() != HitResult.Type.ENTITY) return;
        if (!(mc.hitResult instanceof EntityHitResult ehr)) return;
        Entity e = ehr.getEntity();
        String name = e.getName().getString();
        String extra = "";
        if (e instanceof LivingEntity living) {
            extra = String.format(" %.1fHP", living.getHealth());
        }
        graphics.drawString(mc.font, name + extra, 4, 152, 0xFF00E8A0, false);
    }
}
