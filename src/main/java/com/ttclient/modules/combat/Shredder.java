package com.ttclient.modules.combat;

import com.ttclient.client.TTClientClient;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class Shredder extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Attack range", 6.0, 3.0, 64.0, 0.1));
    public final BoolSetting useRage = addSetting(new BoolSetting("UseRage", "Use Rage reach when on", true));
    public final NumberSetting swordHits = addSetting(new NumberSetting("SwordHits", "Sword attacks per cycle", 4, 1, 12, 1));
    public final NumberSetting maceHits = addSetting(new NumberSetting("MaceHits", "Mace attacks per cycle", 3, 0, 12, 1));
    public final BoolSetting onlyPlayers = addSetting(new BoolSetting("OnlyPlayers", "Only players", true));
    public final BoolSetting requireClick = addSetting(new BoolSetting("RequireClick", "Only while holding attack", false));
    public final BoolSetting auto = addSetting(new BoolSetting("Auto", "Keep shredding nearest", true));
    public final NumberSetting maceFall = addSetting(new NumberSetting("MaceFall", "Fall spoof on mace phase", 40.0, 5.0, 100.0, 1.0));

    private int state, left, targetId = -1;

    public Shredder() {
        super("Shredder", "Paced sword then mace shred", Category.COMBAT);
        addCombatSafetyToggles();
    }

    @Override
    public void onDisable() { state = 0; left = 0; targetId = -1; }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        LocalPlayer p = mc.player;
        boolean click = false;
        try { click = mc.options.keyAttack.isDown(); } catch (Throwable ignored) {}
        if (requireClick.get() && !click && state == 0) return;

        LivingEntity target = null;
        if (targetId >= 0) {
            try {
                Entity e = mc.level.getEntity(targetId);
                if (e instanceof LivingEntity living && living.isAlive()) target = living;
            } catch (Throwable ignored) {}
        }
        if (target == null) {
            target = resolve(mc, p);
            if (target == null) { state = 0; left = 0; return; }
            targetId = target.getId();
        }
        if (!auto.get() && requireClick.get() && !click) { state = 0; left = 0; return; }

        int swordSlot = findSword(p);
        int maceSlot = InvUtils.findHotbarMace(p);
        if (swordSlot < 0 && maceSlot < 0) return;

        if (state == 0) {
            if (swordSlot >= 0 && swordHits.getInt() > 0) {
                state = 1; left = swordHits.getInt();
                InvUtils.selectHotbar(p, swordSlot);
                return;
            }
            if (maceSlot >= 0 && maceHits.getInt() > 0) {
                state = 2; left = maceHits.getInt();
                InvUtils.selectHotbar(p, maceSlot);
                return;
            }
            return;
        }

        lookAt(p, target);
        if (state == 1) {
            if (swordSlot >= 0) InvUtils.selectHotbar(p, swordSlot);
            try { if (p.onGround()) p.setDeltaMovement(p.getDeltaMovement().x, 0.35, p.getDeltaMovement().z); } catch (Throwable ignored) {}
            try { mc.gameMode.attack(p, target); } catch (Throwable ignored) {}
            try { p.swing(InteractionHand.MAIN_HAND); } catch (Throwable ignored) {}
            left--;
            if (left <= 0) {
                if (maceSlot >= 0 && maceHits.getInt() > 0) {
                    state = 2; left = maceHits.getInt();
                    InvUtils.selectHotbar(p, maceSlot);
                } else state = 0;
            }
        } else if (state == 2) {
            if (maceSlot >= 0) InvUtils.selectHotbar(p, maceSlot);
            float f = (float) maceFall.get().doubleValue();
            MaceHack.applySpoof(p, f);
            try { mc.gameMode.attack(p, target); } catch (Throwable ignored) {}
            try { p.swing(InteractionHand.MAIN_HAND); } catch (Throwable ignored) {}
            MaceHack.applySpoof(p, f);
            left--;
            if (left <= 0) {
                if ((auto.get() || click) && swordSlot >= 0) {
                    state = 1; left = swordHits.getInt();
                    InvUtils.selectHotbar(p, swordSlot);
                } else if (auto.get() || click) {
                    state = 2; left = maceHits.getInt();
                } else state = 0;
            }
        }
    }

    private LivingEntity resolve(Minecraft mc, LocalPlayer p) {
        try {
            Entity e = mc.crosshairPickEntity;
            if (e instanceof LivingEntity living && living.isAlive()) {
                if (!onlyPlayers.get() || living instanceof Player) return living;
            }
        } catch (Throwable ignored) {}
        try {
            if (mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living && living.isAlive()) {
                if (!onlyPlayers.get() || living instanceof Player) return living;
            }
        } catch (Throwable ignored) {}
        double r = range.get().doubleValue();
        if (useRage.get() && TTClientClient.modules != null) {
            Rage rage = TTClientClient.modules.getModule(Rage.class);
            if (rage != null && rage.isEnabled()) {
                try { r = Math.max(r, Math.min(rage.reach.get().doubleValue(), 64.0)); } catch (Throwable ignored) {}
            }
        }
        LivingEntity best = null;
        double bestD = r * r;
        Vec3 look = p.getLookAngle();
        Vec3 eye = p.getEyePosition();
        for (Entity e : mc.level.getEntities(p, p.getBoundingBox().inflate(r))) {
            if (!(e instanceof LivingEntity living) || !living.isAlive() || living == p) continue;
            if (onlyPlayers.get() && !(living instanceof Player)) continue;
            Vec3 to = living.position().add(0, living.getBbHeight() * 0.5, 0).subtract(eye);
            if (to.lengthSqr() < 1e-6 || look.dot(to.normalize()) < 0.25) continue;
            double d = p.distanceToSqr(living);
            if (d <= bestD) { bestD = d; best = living; }
        }
        return best;
    }

    private static void lookAt(LocalPlayer p, LivingEntity t) {
        try {
            Vec3 d = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(p.getEyePosition());
            double h = Math.sqrt(d.x * d.x + d.z * d.z);
            float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
            float pitch = (float) (-Math.toDegrees(Math.atan2(d.y, h)));
            p.setYRot(yaw); p.setXRot(pitch); p.yRotO = yaw; p.xRotO = pitch;
        } catch (Throwable ignored) {}
    }

    private static int findSword(LocalPlayer p) {
        int best = -1, score = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.isEmpty()) continue;
            String n = s.getItem().toString().toLowerCase();
            if (!n.contains("sword")) continue;
            int sc = n.contains("netherite") ? 4 : n.contains("diamond") ? 3 : 1;
            if (sc > score) { score = sc; best = i; }
        }
        return best;
    }
}
