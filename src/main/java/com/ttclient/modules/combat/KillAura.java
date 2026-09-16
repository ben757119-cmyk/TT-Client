package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class KillAura extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Attack range", 4.0, 2.0, 6.0, 0.1));
    public final BoolSetting players = addSetting(new BoolSetting("Players", "Target players", true));
    public final BoolSetting mobs = addSetting(new BoolSetting("Mobs", "Target hostiles", true));
    public final NumberSetting cooldown = addSetting(new NumberSetting("Cooldown", "Ticks between hits", 10, 0, 20, 1));
    private int timer;

    public KillAura() {
        super("KillAura", "Attack nearby entities", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (timer > 0) { timer--; return; }

        LocalPlayer p = mc.player;
        double r = range.get();
        AABB box = p.getBoundingBox().inflate(r);
        LivingEntity best = null;
        double bestDist = r * r;

        for (Entity e : mc.level.getEntities(p, box)) {
            if (!(e instanceof LivingEntity living) || !living.isAlive()) continue;
            if (living == p) continue;
            if (living instanceof Player && !players.get()) continue;
            if (living instanceof Monster && !mobs.get()) continue;
            if (!(living instanceof Player) && !(living instanceof Monster)) continue;
            double d = p.distanceToSqr(living);
            if (d < bestDist) {
                bestDist = d;
                best = living;
            }
        }

        if (best != null) {
            mc.gameMode.attack(p, best);
            p.swing(InteractionHand.MAIN_HAND);
            timer = cooldown.getInt();
        }
    }
}
