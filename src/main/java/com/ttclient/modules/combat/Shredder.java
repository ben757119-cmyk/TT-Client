package com.ttclient.modules.combat;

import com.ttclient.client.TTClientClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.InvUtils;
import com.ttclient.util.SilentRotate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Shredder — paced sword then mace combo.
 */
public class Shredder extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Target range", 4.5, 2.0, 8.0, 0.1));
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks between swings", 2, 0, 20, 1));
    public final BoolSetting players = addSetting(new BoolSetting("Players", "Target players", true));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", "Look at target", true));
    public final BoolSetting silent = addSetting(new BoolSetting("Silent", "Silent rotate", true));
    public final BoolSetting swapMace = addSetting(new BoolSetting("SwapMace", "Swap to mace for second hit", true));

    private int tickWait;
    private int phase;

    public Shredder() {
        super("Shredder", "Paced sword then mace shred", Category.COMBAT);
        addCombatSafetyToggles();
    }

    @Override
    public void onEnable() {
        tickWait = 0;
        phase = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (InvUtils.hasScreen(mc)) return;

        if (tickWait > 0) {
            tickWait--;
            return;
        }

        LocalPlayer p = mc.player;
        LivingEntity target = findTarget(mc, p, range.get());
        if (target == null) {
            phase = 0;
            return;
        }

        if (rotate.get()) {
            if (silent.get()) SilentRotate.lookSilent(p, target);
            else {
                float[] a = SilentRotate.anglesTo(p, target);
                p.setYRot(a[0]);
                p.setXRot(a[1]);
            }
        }

        int prev = p.getInventory().getSelectedSlot();
        if (phase == 0) {
            // sword-ish first hit
            int w = InvUtils.findBestWeapon(p);
            if (w >= 0) InvUtils.selectHotbar(p, w);
            if (p.getAttackStrengthScale(0.5f) >= 0.85f) {
                mc.gameMode.attack(p, target);
                p.swing(InteractionHand.MAIN_HAND);
                phase = 1;
                tickWait = delay.getInt();
            }
        } else {
            if (swapMace.get()) {
                int m = InvUtils.findHotbarMace(p);
                if (m >= 0) InvUtils.selectHotbar(p, m);
            }
            if (p.getAttackStrengthScale(0.5f) >= 0.85f) {
                mc.gameMode.attack(p, target);
                p.swing(InteractionHand.MAIN_HAND);
                phase = 0;
                tickWait = delay.getInt();
            }
            InvUtils.selectHotbar(p, prev);
        }
    }

    private LivingEntity findTarget(Minecraft mc, LocalPlayer p, double r) {
        AABB box = p.getBoundingBox().inflate(r);
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : mc.level.getEntities(p, box)) {
            if (!(e instanceof LivingEntity living) || !living.isAlive() || living == p) continue;
            if (players.get() && !(living instanceof Player)) continue;
            if (!players.get() && living instanceof Player) continue;
            double d = p.distanceToSqr(living);
            if (d <= r * r && d < bestD) {
                bestD = d;
                best = living;
            }
        }
        return best;
    }
}
