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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Rage — extreme reach for attacks AND normal block place (not just anchors).
 * Vanilla crosshair pick is short; we re-raycast with full reach when you click.
 */
public class Rage extends Module {
    public final NumberSetting reach = addSetting(new NumberSetting(
            "Reach", "Attack / interact reach", 256.0, 6.0, 1000.0, 1.0));
    public final BoolSetting entities = addSetting(new BoolSetting(
            "Entities", "Boost entity interaction range", true));
    public final BoolSetting blocks = addSetting(new BoolSetting(
            "Blocks", "Boost block interaction range + long place", true));
    public final BoolSetting longHit = addSetting(new BoolSetting(
            "LongHit", "Attack entities at full reach on click", true));
    public final BoolSetting longPlace = addSetting(new BoolSetting(
            "LongPlace", "Place blocks at full reach on use", true));
    public final BoolSetting autoHit = addSetting(new BoolSetting(
            "AutoHit", "Also auto-attack nearest in range", false));
    public final BoolSetting playersOnly = addSetting(new BoolSetting(
            "PlayersOnly", "Only players for auto/long hit", false));
    public final NumberSetting cooldown = addSetting(new NumberSetting(
            "Cooldown", "Ticks between auto attacks", 8, 0, 40, 1));

    private double oldEntity = 3.0;
    private double oldBlock = 4.5;
    private int timer;

    public Rage() {
        super("Rage", "Long reach hits + place (not only anchors)", Category.COMBAT);
        addCombatSafetyToggles();
    }

    @Override
    public void onEnable() { applyAttributes(); }

    @Override
    public void onDisable() { restoreAttributes(); }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        LocalPlayer p = mc.player;

        applyAttributes();

        double r = Math.min(reach.get(), 64.0);

        boolean attackDown = false;
        boolean useDown = false;
        try {
            attackDown = mc.options.keyAttack.isDown();
            useDown = mc.options.keyUse.isDown();
        } catch (Throwable ignored) {}

        if (longHit.get() && attackDown && entities.get()) {
            LivingEntity target = rayEntity(mc, p, r);
            if (target == null) target = nearestInView(mc, p, r);
            if (target != null) {
                try {
                    mc.gameMode.attack(p, target);
                    p.swing(InteractionHand.MAIN_HAND);
                } catch (Throwable ignored) {}
            }
        }

        if (longPlace.get() && useDown && blocks.get()) {
            ItemStack held = p.getMainHandItem();
            if (!held.isEmpty() && held.getItem() instanceof BlockItem) {
                try {
                    Vec3 eye = p.getEyePosition(1.0f);
                    Vec3 look = p.getViewVector(1.0f);
                    Vec3 end = eye.add(look.scale(r));
                    BlockHitResult hit = mc.level.clip(new ClipContext(
                            eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
                    if (hit.getType() == HitResult.Type.BLOCK) {
                        mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, hit);
                    }
                } catch (Throwable ignored) {}
            }
        }

        if (autoHit.get()) {
            if (timer > 0) {
                timer--;
            } else {
                LivingEntity target = nearestInView(mc, p, r);
                if (target != null && p.getAttackStrengthScale(0.5f) >= 0.9f) {
                    try {
                        mc.gameMode.attack(p, target);
                        p.swing(InteractionHand.MAIN_HAND);
                    } catch (Throwable ignored) {}
                    timer = cooldown.getInt();
                }
            }
        }
    }

    private LivingEntity rayEntity(Minecraft mc, LocalPlayer p, double r) {
        try {
            Vec3 eye = p.getEyePosition(1.0f);
            Vec3 look = p.getViewVector(1.0f);
            Vec3 end = eye.add(look.scale(r));
            EntityHitResult ehr = null;
            // Prefer projectile util if available via pick
            HitResult hr = mc.hitResult;
            if (hr instanceof EntityHitResult er && er.getEntity() instanceof LivingEntity le) {
                if (le.isAlive() && le != p) {
                    if (!playersOnly.get() || le instanceof Player) return le;
                }
            }
            return nearestInView(mc, p, r);
        } catch (Throwable t) {
            return null;
        }
    }

    private LivingEntity nearestInView(Minecraft mc, LocalPlayer p, double r) {
        AABB box = p.getBoundingBox().inflate(r);
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : mc.level.getEntities(p, box)) {
            if (!(e instanceof LivingEntity living) || !living.isAlive() || living == p) continue;
            if (playersOnly.get() && !(living instanceof Player)) continue;
            double d = p.distanceToSqr(living);
            if (d <= r * r && d < bestD) {
                bestD = d;
                best = living;
            }
        }
        return best;
    }

    private void applyAttributes() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        try {
            if (entities.get()) {
                AttributeInstance a = mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
                if (a != null) {
                    if (oldEntity == 3.0) oldEntity = a.getBaseValue();
                    a.setBaseValue(reach.get());
                }
            }
            if (blocks.get()) {
                AttributeInstance a = mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
                if (a != null) {
                    if (oldBlock == 4.5) oldBlock = a.getBaseValue();
                    a.setBaseValue(reach.get());
                }
            }
        } catch (Throwable ignored) {}
    }

    private void restoreAttributes() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        try {
            AttributeInstance e = mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
            if (e != null) e.setBaseValue(oldEntity);
            AttributeInstance b = mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
            if (b != null) b.setBaseValue(oldBlock);
        } catch (Throwable ignored) {}
    }
}
