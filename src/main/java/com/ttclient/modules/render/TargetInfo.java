package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class TargetInfo extends Module {
    public TargetInfo() {
        super("TargetInfo", "Show looked-at entity info", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor graphics, float partialTick) {
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.player == null || minecraft.hitResult == null) return;
        if (minecraft.hitResult.getType() != HitResult.Type.ENTITY) return;
        if (!(minecraft.hitResult instanceof EntityHitResult ehr)) return;
        Entity e = ehr.getEntity();
        String name = e.getName().getString();
        String extra = "";
        if (e instanceof LivingEntity living) {
            extra = String.format(" %.1fHP", living.getHealth());
        }
        graphics.text(minecraft.font, name + extra, 4, 152, 0xFF00E8A0, false);
    }
}
