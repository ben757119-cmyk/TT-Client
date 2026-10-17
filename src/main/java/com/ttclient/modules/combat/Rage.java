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
            if (held.getItem() instanceof BlockItem) {
                BlockHitResult bhr = rayBlock(mc, p, r);
                if (bhr != null && bhr.getType() == HitResult.Type.BLOCK) {
                    try {
                        mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, bhr);
                        p.swing(InteractionHand.MAIN_HAND);
                    } catch (Throwable ignored) {}
                }
            }
        }

        if (autoHit.get()) {
            if (timer > 0) {
                timer--;
            } else {
                LivingEntity target = nearestInView(mc, p, r);
                if (target != null) {
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
            Vec3 eye = p.getEyePosition();
            Vec3 look = p.getLookAngle();
            Vec3 end = eye.add(look.scale(r));
            AABB box = p.getBoundingBox().expandTowards(look.scale(r)).inflate(1.0);
            LivingEntity best = null;
            double bestD = r * r;
            for (Entity e : mc.level.getEntities(p, box)) {
                if (!(e instanceof LivingEntity living) || !living.isAlive() || living == p) continue;
                if (playersOnly.get() && !(living instanceof Player)) continue;
                AABB bb = living.getBoundingBox().inflate(0.15);
                var opt = bb.clip(eye, end);
                if (opt.isEmpty()) continue;
                double d = eye.distanceToSqr(opt.get());
                if (d < bestD) { bestD = d; best = living; }
            }
            return best;
        } catch (Throwable t) {
            return null;
        }
    }

    private LivingEntity nearestInView(Minecraft mc, LocalPlayer p, double r) {
        LivingEntity best = null;
        double bestD = r * r;
        AABB box = p.getBoundingBox().inflate(r);
        Vec3 look = p.getLookAngle();
        Vec3 eye = p.getEyePosition();
        for (Entity e : mc.level.getEntities(p, box)) {
            if (!(e instanceof LivingEntity living) || !living.isAlive() || living == p) continue;
            if (playersOnly.get() && !(living instanceof Player)) continue;
            Vec3 to = living.position().add(0, living.getBbHeight() * 0.5, 0).subtract(eye);
            if (to.lengthSqr() < 1.0e-6) continue;
            if (look.dot(to.normalize()) < 0.5) continue;
            double d = p.distanceToSqr(living);
            if (d <= bestD) { bestD = d; best = living; }
        }
        return best;
    }

    private BlockHitResult rayBlock(Minecraft mc, LocalPlayer p, double r) {
        try {
            Vec3 eye = p.getEyePosition();
            Vec3 end = eye.add(p.getLookAngle().scale(r));
            ClipContext ctx = new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p);
            HitResult hr = mc.level.clip(ctx);
            if (hr instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) return bhr;
        } catch (Throwable ignored) {}
        return null;
    }

    private void applyAttributes() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        double r = reach.get();
        if (entities.get()) setAttr(mc.player, true, r);
        if (blocks.get()) setAttr(mc.player, false, r);
    }

    private void setAttr(LocalPlayer p, boolean entity, double r) {
        try {
            AttributeInstance inst = p.getAttribute(
                    entity ? Attributes.ENTITY_INTERACTION_RANGE : Attributes.BLOCK_INTERACTION_RANGE);
            if (inst == null) return;
            double cur = inst.getBaseValue();
            if (entity) { if (cur < 10) oldEntity = cur; }
            else { if (cur < 10) oldBlock = cur; }
            try { inst.setBaseValue(r); }
            catch (Throwable t) {
                try { inst.setBaseValue(Math.min(r, 64)); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
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
}
