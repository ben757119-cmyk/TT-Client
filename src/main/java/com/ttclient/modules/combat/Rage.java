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
            "PlayersOnly", "AutoHit only targets players", true));

    public Rage() {
        super("Rage", "Long reach hits + place (not only anchors)", Category.COMBAT);
        addCombatSafetyToggles();
    }

    @Override
    public void onEnable() { applyReach(); }

    @Override
    public void onDisable() { clearReach(); }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        applyReach();
        LocalPlayer p = mc.player;

        if (longHit.get() && mc.options.keyAttack.isDown()) {
            LivingEntity target = findTarget(mc, p, reach.get());
            if (target != null && p.getAttackStrengthScale(0.5f) >= 0.9f) {
                mc.gameMode.attack(p, target);
                p.swing(InteractionHand.MAIN_HAND);
            }
        }

        if (autoHit.get()) {
            LivingEntity target = findTarget(mc, p, reach.get());
            if (target != null && p.getAttackStrengthScale(0.5f) >= 0.92f) {
                mc.gameMode.attack(p, target);
                p.swing(InteractionHand.MAIN_HAND);
            }
        }

        if (longPlace.get() && mc.options.keyUse.isDown()) {
            ItemStack held = p.getMainHandItem();
            if (!held.isEmpty() && held.getItem() instanceof BlockItem) {
                try {
                    double r = reach.get();
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
    }

    private void applyReach() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        try {
            if (entities.get()) {
                AttributeInstance a = mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
                if (a != null) a.setBaseValue(reach.get());
            }
            if (blocks.get()) {
                AttributeInstance a = mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
                if (a != null) a.setBaseValue(reach.get());
            }
        } catch (Throwable ignored) {}
    }

    private void clearReach() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        try {
            AttributeInstance e = mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
            if (e != null) e.setBaseValue(3.0);
            AttributeInstance b = mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
            if (b != null) b.setBaseValue(4.5);
        } catch (Throwable ignored) {}
    }

    private LivingEntity findTarget(Minecraft mc, LocalPlayer p, double r) {
        AABB box = p.getBoundingBox().inflate(r);
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : mc.level.getEntities(p, box)) {
            if (!(e instanceof LivingEntity living) || !living.isAlive() || living == p) continue;
            if (playersOnly.get() && !(living instanceof Player)) continue;
            double d = p.distanceToSqr(living);
            if (d < bestD && d <= r * r) {
                bestD = d;
                best = living;
            }
        }
        return best;
    }
}
