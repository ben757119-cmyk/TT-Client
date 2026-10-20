package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.InvUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** Mace aura — calls MaceHack packet spoof before each hit when MaceHack is on. */
public class MaceAura extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Attack range", 4.5, 2, 6, 0.1));
    public final NumberSetting cooldown = addSetting(new NumberSetting("Cooldown", "Ticks between hits", 12, 0, 30, 1));
    public final BoolSetting onlyPlayers = addSetting(new BoolSetting("PlayersOnly", "Only players", true));
    public final BoolSetting requireFall = addSetting(new BoolSetting("RequireFall", "Only while falling (ignored if MaceHack on)", true));
    public final BoolSetting autoSwap = addSetting(new BoolSetting("AutoSwap", "Switch to mace", true));
    public final NumberSetting minFall = addSetting(new NumberSetting("MinFall", "Min fall distance", 1.5, 0, 20, 0.5));

    private int timer;
    private int prevSlot = -1;

    public MaceAura() {
        super("MaceAura", "Hit with mace for smash attacks", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        Minecraft mc = mc();
        if (mc != null && mc.player != null && prevSlot >= 0) {
            InvUtils.selectHotbar(mc.player, prevSlot);
        }
        prevSlot = -1;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (timer > 0) { timer--; return; }

        LocalPlayer p = mc.player;
        boolean hack = MaceHack.isActive();

        if (requireFall.get() && !hack) {
            boolean airborne = !p.onGround();
            boolean falling = p.fallDistance >= minFall.get() || p.getDeltaMovement().y < -0.1;
            if (!airborne && !falling) return;
        }

        int mace = InvUtils.findHotbarMace(p);
        if (mace < 0) return;

        LivingEntity target = findTarget(mc, p);
        if (target == null) return;

        if (autoSwap.get()) {
            if (prevSlot < 0) prevSlot = p.getInventory().getSelectedSlot();
            InvUtils.selectHotbar(p, mace);
        } else if (!InvUtils.isMace(p.getMainHandItem())) {
            return;
        }

        // Critical: packet fall spoof before attack (not just client fallDistance)
        if (hack) {
            MaceHack.spoofBeforeAttack(p);
        }

        try { mc.gameMode.attack(p, target); } catch (Throwable ignored) {}
        try { p.swing(InteractionHand.MAIN_HAND); } catch (Throwable ignored) {}

        timer = cooldown.getInt();
    }

    private LivingEntity findTarget(Minecraft mc, LocalPlayer p) {
        double r = range.get();
        LivingEntity best = null;
        double bestD = r * r;
        AABB box = p.getBoundingBox().inflate(r);
        for (Entity e : mc.level.getEntities(p, box)) {
            if (!(e instanceof LivingEntity living) || !living.isAlive() || living == p) continue;
            if (onlyPlayers.get() && !(living instanceof Player)) continue;
            double d = p.distanceToSqr(living);
            if (d < bestD) { bestD = d; best = living; }
        }
        return best;
    }
}
