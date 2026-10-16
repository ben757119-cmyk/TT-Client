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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Rage — extreme reach.
 *
 * 1) Sets ENTITY_INTERACTION_RANGE / BLOCK_INTERACTION_RANGE attributes
 *    (vanilla hard-cap is 64; we still request the configured value).
 * 2) Optional auto-hit: attacks the nearest living entity within range via
 *    gameMode.attack so targeting is not limited to the crosshair ray.
 *
 * Singleplayer / weak servers: works. Strict servers may still cancel far hits.
 */
public class Rage extends Module {
    public final NumberSetting reach = addSetting(new NumberSetting(
            "Reach", "Attack / interact reach", 256.0, 6.0, 256.0, 1.0));
    public final BoolSetting entities = addSetting(new BoolSetting(
            "Entities", "Boost entity interaction range attribute", true));
    public final BoolSetting blocks = addSetting(new BoolSetting(
            "Blocks", "Boost block interaction range attribute", true));
    public final BoolSetting autoHit = addSetting(new BoolSetting(
            "AutoHit", "Attack nearest entity in reach (not just crosshair)", true));
    public final BoolSetting playersOnly = addSetting(new BoolSetting(
            "PlayersOnly", "AutoHit only players", false));
    public final NumberSetting cooldown = addSetting(new NumberSetting(
            "Cooldown", "Ticks between AutoHit attacks", 10, 0, 40, 1));

    private double oldEntity = 3.0;
    private double oldBlock = 4.5;
    private int timer;

    public Rage() {
        super("Rage", "256 reach — attribute + long-range attacks", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        applyAttributes();
    }

    @Override
    public void onDisable() {
        restoreAttributes();
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;

        // Keep attributes forced every tick (other mods / respawn can reset)
        applyAttributes();

        if (!autoHit.get() || mc.gameMode == null || mc.level == null) return;
        if (timer > 0) { timer--; return; }

        LocalPlayer p = mc.player;
        double r = reach.get();
        LivingEntity target = findTarget(mc, p, r);
        if (target == null) return;

        try {
            mc.gameMode.attack(p, target);
            p.swing(InteractionHand.MAIN_HAND);
        } catch (Throwable ignored) {}
        timer = cooldown.getInt();
    }

    private void applyAttributes() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        LocalPlayer p = mc.player;
        double r = reach.get();

        if (entities.get()) {
            AttributeInstance inst = p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
            if (inst != null) {
                if (oldEntity == 3.0 || inst.getBaseValue() != r) {
                    // remember first non-rage base if still default-ish
                    double cur = inst.getBaseValue();
                    if (cur < 10) oldEntity = cur;
                }
                try {
                    inst.setBaseValue(r);
                } catch (Throwable t) {
                    // Some builds clamp to attribute max (64)
                    try { inst.setBaseValue(Math.min(r, 64)); } catch (Throwable ignored) {}
                }
            }
        }

        if (blocks.get()) {
            AttributeInstance inst = p.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
            if (inst != null) {
                double cur = inst.getBaseValue();
                if (cur < 10) oldBlock = cur;
                try {
                    inst.setBaseValue(r);
                } catch (Throwable t) {
                    try { inst.setBaseValue(Math.min(r, 64)); } catch (Throwable ignored) {}
                }
            }
        }
    }

    private void restoreAttributes() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        LocalPlayer p = mc.player;
        try {
            AttributeInstance e = p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
            if (e != null) e.setBaseValue(oldEntity);
        } catch (Throwable ignored) {}
        try {
            AttributeInstance b = p.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
            if (b != null) b.setBaseValue(oldBlock);
        } catch (Throwable ignored) {}
    }

    private LivingEntity findTarget(Minecraft mc, LocalPlayer self, double r) {
        LivingEntity best = null;
        double bestD = r * r;
        AABB box = self.getBoundingBox().inflate(r);
        for (Entity e : mc.level.getEntities(self, box)) {
            if (!(e instanceof LivingEntity living) || !living.isAlive() || living == self) continue;
            if (playersOnly.get() && !(living instanceof Player)) continue;
            // skip friendly animals if playersOnly is off? allow all living
            double d = self.distanceToSqr(living);
            if (d <= bestD) {
                bestD = d;
                best = living;
            }
        }
        return best;
    }
}
