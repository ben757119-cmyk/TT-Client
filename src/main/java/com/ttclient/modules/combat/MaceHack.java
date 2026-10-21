package com.ttclient.modules.combat;

import com.ttclient.client.TTClientClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.InvUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Full Wurst MaceDMG port for NeoForge 26.2.
 *
 * Critical: packets must leave BEFORE ServerboundInteractPacket.
 * MixinMultiPlayerGameMode injects at attack() HEAD for the vanilla click path.
 * Auto mode still spoofs then calls gameMode.attack.
 *
 * Sequence (see ServerGamePacketListenerImpl.handleMovePlayer):
 *   4× Y+0 onGround=false → 1× Y+√500 → 1× Y+0
 */
public class MaceHack extends Module {
    public final BoolSetting onlyPlayers = addSetting(new BoolSetting(
            "OnlyPlayers", "Only players", false));
    public final NumberSetting range = addSetting(new NumberSetting(
            "Range", "Auto target range", 6.0, 2.0, 16.0, 0.1));
    public final BoolSetting useRage = addSetting(new BoolSetting(
            "UseRage", "Use Rage reach when on", true));
    public final BoolSetting auto = addSetting(new BoolSetting(
            "Auto", "Auto-attack nearest while holding mace", false));
    public final NumberSetting cooldown = addSetting(new NumberSetting(
            "Cooldown", "Ticks between auto hits", 10, 0, 40, 1));
    /** Always on for vanilla left-click — mixin path. */
    public final BoolSetting onClick = addSetting(new BoolSetting(
            "OnClick", "Spoof on every left-click attack (recommended)", true));

    private int timer;
    private static volatile long lastSpoofMs;

    public MaceHack() {
        super("MaceHack", "Mace smash via Wurst fall-packet spoof", Category.COMBAT);
    }

    public static boolean isActive() {
        try {
            var mods = TTClientClient.modules;
            if (mods == null) return false;
            MaceHack h = mods.getModule(MaceHack.class);
            return h != null && h.isEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Called from MixinMultiPlayerGameMode at attack HEAD — packets first, then
     * vanilla continues and sends the interact packet.
     */
    public static void onPlayerAttack(Player attacker, Entity target) {
        try {
            if (attacker == null) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null || attacker != mc.player) return;

            var mods = TTClientClient.modules;
            if (mods == null) return;
            MaceHack h = mods.getModule(MaceHack.class);
            if (h == null || !h.isEnabled()) return;
            if (!h.onClick.get()) return;
            if (!holdingMace(mc.player)) return;
            if (h.onlyPlayers.get() && !(target instanceof Player)) return;

            // Debounce double-fire (mixin + auto same tick)
            long now = System.currentTimeMillis();
            if (now - lastSpoofMs < 40) return;
            lastSpoofMs = now;

            sendWurstSequence(mc.player);
        } catch (Throwable ignored) {}
    }

    public static void spoofBeforeAttack(LocalPlayer p) {
        if (p == null || !isActive() || !holdingMace(p)) return;
        long now = System.currentTimeMillis();
        if (now - lastSpoofMs < 40) return;
        lastSpoofMs = now;
        sendWurstSequence(p);
    }

    private static boolean holdingMace(LocalPlayer p) {
        try {
            if (p.getMainHandItem().is(Items.MACE)) return true;
        } catch (Throwable ignored) {}
        try {
            return InvUtils.isMace(p.getMainHandItem());
        } catch (Throwable t) {
            return false;
        }
    }

    /** Exact Wurst sendFakeY sequence. */
    private static void sendWurstSequence(LocalPlayer p) {
        if (p == null || p.connection == null) return;
        double x = p.getX();
        double y = p.getY();
        double z = p.getZ();
        boolean horiz = false;
        try { horiz = p.horizontalCollision; } catch (Throwable ignored) {}

        double peak = Math.sqrt(500.0); // ~22.360679774997898

        for (int i = 0; i < 4; i++) {
            sendPos(p, x, y, z, false, horiz);
        }
        sendPos(p, x, y + peak, z, false, horiz);
        sendPos(p, x, y, z, false, horiz);
    }

    private static void sendPos(LocalPlayer p, double x, double y, double z, boolean onGround, boolean horiz) {
        // Direct mapped ctor (works on 26.2 / same as Wurst)
        try {
            p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, onGround, horiz));
            return;
        } catch (Throwable ignored) {}
        try {
            p.connection.send(new ServerboundMovePlayerPacket.Pos(new Vec3(x, y, z), onGround, horiz));
            return;
        } catch (Throwable ignored) {}
        try {
            Class<?> posCl = Class.forName("net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$Pos");
            for (var c : posCl.getConstructors()) {
                Class<?>[] pt = c.getParameterTypes();
                Object pkt = null;
                try {
                    if (pt.length == 5 && pt[0] == double.class) {
                        pkt = c.newInstance(x, y, z, onGround, horiz);
                    } else if (pt.length == 3 && Vec3.class.isAssignableFrom(pt[0])) {
                        pkt = c.newInstance(new Vec3(x, y, z), onGround, horiz);
                    } else if (pt.length == 4 && pt[0] == double.class) {
                        pkt = c.newInstance(x, y, z, onGround);
                    }
                } catch (Throwable ignored2) {}
                if (pkt != null) {
                    p.connection.send((net.minecraft.network.protocol.Packet<?>) pkt);
                    return;
                }
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void onDisable() {
        timer = 0;
    }

    @Override
    public void onTick() {
        // Vanilla clicks are handled by the mixin — only Auto path here
        if (!auto.get()) return;

        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        LocalPlayer p = mc.player;

        if (timer > 0) {
            timer--;
            return;
        }

        if (!holdingMace(p)) {
            int slot = InvUtils.findHotbarMace(p);
            if (slot >= 0) InvUtils.selectHotbar(p, slot);
            else return;
        }

        LivingEntity target = resolveTarget(mc, p);
        if (target == null) return;

        long now = System.currentTimeMillis();
        if (now - lastSpoofMs >= 40) {
            lastSpoofMs = now;
            sendWurstSequence(p);
        }

        try { mc.gameMode.attack(p, target); } catch (Throwable ignored) {}
        try { p.swing(InteractionHand.MAIN_HAND); } catch (Throwable ignored) {}
        timer = Math.max(1, cooldown.getInt());
    }

    private LivingEntity resolveTarget(Minecraft mc, LocalPlayer p) {
        try {
            Entity e = mc.crosshairPickEntity;
            if (e instanceof LivingEntity living && living.isAlive()) {
                if (onlyPlayers.get() && !(living instanceof Player)) { /* skip */ }
                else return living;
            }
        } catch (Throwable ignored) {}
        try {
            HitResult hr = mc.hitResult;
            if (hr instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living && living.isAlive()) {
                if (!(onlyPlayers.get() && !(living instanceof Player))) return living;
            }
        } catch (Throwable ignored) {}

        double r = range.get().doubleValue();
        if (useRage.get() && TTClientClient.modules != null) {
            try {
                Rage rage = TTClientClient.modules.getModule(Rage.class);
                if (rage != null && rage.isEnabled()) {
                    r = Math.max(r, Math.min(rage.reach.get().doubleValue(), 16.0));
                }
            } catch (Throwable ignored) {}
        }

        LivingEntity best = null;
        double bestD = r * r;
        AABB box = p.getBoundingBox().inflate(r);
        Vec3 look = p.getLookAngle();
        Vec3 eye = p.getEyePosition();
        for (Entity e : mc.level.getEntities(p, box)) {
            if (!(e instanceof LivingEntity living) || !living.isAlive() || living == p) continue;
            if (onlyPlayers.get() && !(living instanceof Player)) continue;
            Vec3 to = living.position().add(0, living.getBbHeight() * 0.5, 0).subtract(eye);
            if (to.lengthSqr() < 1e-6) continue;
            if (look.dot(to.normalize()) < 0.12) continue;
            double d = p.distanceToSqr(living);
            if (d <= bestD) { bestD = d; best = living; }
        }
        return best;
    }
}
