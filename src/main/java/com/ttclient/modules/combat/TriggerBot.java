package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class TriggerBot extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks between attacks", 2, 0, 10, 1));
    private int timer;

    public TriggerBot() {
        super("TriggerBot", "Attack when crosshair is on an entity", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null) return;
        if (timer > 0) { timer--; return; }
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) return;
        Entity e = ((EntityHitResult) hit).getEntity();
        if (!(e instanceof LivingEntity living) || !living.isAlive()) return;
        mc.gameMode.attack(mc.player, living);
        mc.player.swing(InteractionHand.MAIN_HAND);
        timer = delay.getInt();
    }
}
