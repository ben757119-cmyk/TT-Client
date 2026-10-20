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
 * Mace smash damage — server-side fall distance spoof.
 *
 * Vanilla mace smash requires fallDistance &gt; 1.5 on the *server*.
 * Client-only setFallDistance does nothing useful.
 *
 * Wurst MaceDMG approach (proven on 1.21):
 *   before attack, send Pos packets that accumulate fall on the server:
 *     4x same Y onGround=false, 1x Y+sqrt(500) (~22.4), 1x same Y.
 * See ServerGamePacketListenerImpl.handleMovePlayer for the server check.
 */
public class MaceHack extends Module {
    public final NumberSetting height = addSetting(new NumberSetting(
            "Height", "Spoofed fall height (blocks). ~22 = max useful smash", 22.0, 2.0, 50.0, 0.5));
    public final BoolSetting onlyPlayers = addSetting(new BoolSetting(
            "OnlyPlayers", "Only players", true));
    public final NumberSetting range = addSetting(new NumberSetting(
            "Range", "Target range", 6.0, 2.0, 16.0, 0.1));
    public final BoolSetting useRage = addSetting(new BoolSetting(
            "UseRage", "Use Rage reach when on", true));
    public final BoolSetting auto = addSetting(new BoolSetting(
            "Auto", "Auto-attack nearest target while holding mace", false));
    public final NumberSetting cooldown = addSetting(new NumberSetting(
            "Cooldown", "Ticks between auto hits", 12, 0, 40, 1));
    public final BoolSetting onClick = addSetting(new BoolSetting(
            "OnClick", "Spoof when you left-click (recommended)", true));

    private int timer;
    private boolean wasClick;

    public MaceHack() {
        super("MaceHack", "Mace smash on ground via packet fall spoof", Category.COMBAT);
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

    /** Public entry so MaceAura / other combat can spoof before their own attack. */
    public static void spoofBeforeAttack(LocalPlayer p) {
        try {
            var mods = TTClientClient.modules;
            if (mods == null) return;
            MaceHack h = mods.getModule(MaceHack.class);
            if (h == null || !h.isEnabled()) return;
            h.sendFallPackets(p, h.height.get());
        } catch (Throwable ignored) {}
    }

    public static float spoofAmount() {
        try {
            var mods = TTClientClient.modules;
            if (mods == null) return 22f;
            MaceHack h = mods.getModule(MaceHack.class);
            if (h == null || !h.isEnabled()) return 22f;
            return (float) h.height.get();
        } catch (Throwable t) {
            return 22f;
        }
    }

    /** @deprecated use spoofBeforeAttack — client fall alone does not smash */
    public static void applySpoof(LocalPlayer p, float fallDist) {
        spoofBeforeAttack(p);
    }

    @Override
    public void onDisable() {
        timer = 0;
        wasClick = false;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        LocalPlayer p = mc.player;

        if (timer > 0) timer--;

        if (!holdingMace(p)) {
            int slot = InvUtils.findHotbarMace(p);
            if (slot >= 0 && auto.get()) {
                InvUtils.selectHotbar(p, slot);
            } else {
                wasClick = false;
                return;
            }
        }

        boolean click = false;
        try { click = mc.options.keyAttack.isDown(); } catch (Throwable ignored) {}
        boolean edge = click && !wasClick;
        wasClick = click;

        boolean shouldHit = false;
        if (onClick.get() && edge) shouldHit = true;
        if (auto.get() && timer <= 0) shouldHit = true;
        if (!shouldHit) return;

        LivingEntity target = resolveTarget(mc, p);
        if (target == null) return;

        // Packet fall spoof FIRST, then attack — server accumulates fallDistance
        sendFallPackets(p, height.get());

        // Client-side state (helps local prediction / particles)
        setClientFall(p, (float) height.get());

        try { mc.gameMode.attack(p, target); } catch (Throwable ignored) {}
        try { p.swing(InteractionHand.MAIN_HAND); } catch (Throwable ignored) {}

        timer = Math.max(1, cooldown.getInt());
    }

    /**
     * Wurst MaceDMG packet sequence adapted for NeoForge 26.2.
     * ServerGamePacketListenerImpl accumulates fall when onGround=false moves arrive.
     */
    private void sendFallPackets(LocalPlayer p, double fallHeight) {
        if (p == null || p.connection == null) return;
        double x = p.getX();
        double y = p.getY();
        double z = p.getZ();
        boolean horiz = false;
        try { horiz = p.horizontalCollision; } catch (Throwable ignored) {}

        // Wurst: 4x zero offset, then sqrt(500)~22.36, then zero.
        // Generalize: use configured height (clamped) instead of fixed sqrt(500).
        double up = Math.max(1.5, Math.min(fallHeight, 50.0));
        // Paper/vanilla soft-cap style: small steps then big jump
        for (int i = 0; i < 4; i++) {
            sendPos(p, x, y, z, false, horiz);
        }
        sendPos(p, x, y + up, z, false, horiz);
        sendPos(p, x, y, z, false, horiz);
    }

    private void sendPos(LocalPlayer p, double x, double y, double z, boolean onGround, boolean horiz) {
        try {
            // 26.2: ServerboundMovePlayerPacket.Pos(x, y, z, onGround, horizontalCollision)
            p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, onGround, horiz));
            return;
        } catch (Throwable ignored) {}
        try {
            // Fallback: Pos(Vec3, onGround, horizontalCollision)
            p.connection.send(new ServerboundMovePlayerPacket.Pos(new Vec3(x, y, z), onGround, horiz));
            return;
        } catch (Throwable ignored) {}
        // Reflection fallback for mapping drift
        try {
            Class<?> posCl = Class.forName("net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$Pos");
            for (var c : posCl.getConstructors()) {
                Class<?>[] pt = c.getParameterTypes();
                Object pkt = null;
                if (pt.length == 5 && pt[0] == double.class) {
                    pkt = c.newInstance(x, y, z, onGround, horiz);
                } else if (pt.length == 3 && Vec3.class.isAssignableFrom(pt[0])) {
                    pkt = c.newInstance(new Vec3(x, y, z), onGround, horiz);
                } else if (pt.length == 4 && pt[0] == double.class) {
                    // older (x,y,z,onGround)
                    pkt = c.newInstance(x, y, z, onGround);
                }
                if (pkt != null) {
                    p.connection.send((net.minecraft.network.protocol.Packet<?>) pkt);
                    return;
                }
            }
        } catch (Throwable ignored) {}
    }

    private void setClientFall(LocalPlayer p, float fallDist) {
        try {
            var m = LivingEntity.class.getMethod("setFallDistance", float.class);
            m.invoke(p, fallDist);
            return;
        } catch (Throwable ignored) {}
        try {
            for (Class<?> c = p.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (var f : c.getDeclaredFields()) {
                    if (f.getType() != float.class) continue;
                    String n = f.getName().toLowerCase();
                    if (n.contains("fall")) {
                        f.setAccessible(true);
                        f.setFloat(p, fallDist);
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {}
        try { p.setOnGround(false); } catch (Throwable ignored) {}
    }

    private boolean holdingMace(LocalPlayer p) {
        try {
            if (p.getMainHandItem().is(Items.MACE)) return true;
        } catch (Throwable ignored) {}
        return InvUtils.isMace(p.getMainHandItem());
    }

    private LivingEntity resolveTarget(Minecraft mc, LocalPlayer p) {
        LivingEntity cross = crosshair(mc);
        if (cross != null) return cross;
        double r = effectiveRange();
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
            if (look.dot(to.normalize()) < 0.15) continue;
            double d = p.distanceToSqr(living);
            if (d <= bestD) { bestD = d; best = living; }
        }
        return best;
    }

    private double effectiveRange() {
        double r = range.get();
        if (useRage.get() && TTClientClient.modules != null) {
            Rage rage = TTClientClient.modules.getModule(Rage.class);
            if (rage != null && rage.isEnabled()) {
                try { r = Math.max(r, Math.min(rage.reach.get(), 16.0)); }
                catch (Throwable ignored) {}
            }
        }
        return r;
    }

    private LivingEntity crosshair(Minecraft mc) {
        try {
            Entity e = mc.crosshairPickEntity;
            if (e instanceof LivingEntity living && living.isAlive()) {
                if (onlyPlayers.get() && !(living instanceof Player)) return null;
                return living;
            }
        } catch (Throwable ignored) {}
        try {
            HitResult hr = mc.hitResult;
            if (hr instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living && living.isAlive()) {
                if (onlyPlayers.get() && !(living instanceof Player)) return null;
                return living;
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
