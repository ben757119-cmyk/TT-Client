package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.FriendsManager;
import com.ttclient.util.SilentRotate;
import com.ttclient.util.TimerHooks;
import com.ttclient.util.StorkFx;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Random;

/** Im Storking It — rise, spin, TP-aura strike. */
public class ImStorkingIt extends Module {
    public final NumberSetting height = addSetting(new NumberSetting("Height", "Blocks above ground to hover", 40.0, 8.0, 80.0, 1.0));
    public final NumberSetting riseSpeed = addSetting(new NumberSetting("RiseSpeed", "Vertical rise speed", 0.25, 0.05, 1.0, 0.05));
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Target search range", 256.0, 16.0, 256.0, 1.0));
    public final NumberSetting hopSize = addSetting(new NumberSetting("Hop", "Max blocks per move packet (servers reject >=10)", 8.5, 2.0, 9.5, 0.1));
    public final NumberSetting strikeDist = addSetting(new NumberSetting("StrikeDist", "How close hops end to the target", 2.5, 1.0, 4.0, 0.1));
    public final NumberSetting cps = addSetting(new NumberSetting("CPS", "Hits per second (real-time)", 12.0, 1.0, 20.0, 0.5));
    public final NumberSetting minCharge = addSetting(new NumberSetting("MinCharge", "Min attack strength scale", 0.9, 0.0, 1.0, 0.05));
    public final NumberSetting timerSpeed = addSetting(new NumberSetting("Timer", "Client timer multiplier while striking", 8.0, 1.0, 10.0, 0.5));
    public final NumberSetting introTicks = addSetting(new NumberSetting("IntroTicks", "Hover ticks before rise", 32, 0, 80, 1));
    public final NumberSetting freezeTicks = addSetting(new NumberSetting("FreezeTicks", "Hold at top before striking", 20, 0, 100, 1));
    public final BoolSetting onlyPlayers = addSetting(new BoolSetting("OnlyPlayers", "Only target players", true));
    public final BoolSetting ignoreFriends = addSetting(new BoolSetting("IgnoreFriends", "Skip friends", true));
    public final BoolSetting visualBolt = addSetting(new BoolSetting("VisualBolt", "Client-only lightning on hit", true));
    public final BoolSetting silentSpin = addSetting(new BoolSetting("SilentSpin", "Spin body without moving camera", true));

    private static final int IDLE = 0, INTRO = 1, RISING = 2, FROZEN = 3, STRIKING = 4;
    private static final float MIN_SPIN = 4.0f, MAX_SPIN = 45.0f;
    private static final long BOLT_EVERY_NANOS = 150_000_000L;
    private static final long SHAKE_NANOS = 280_000_000L;
    private static final float SHAKE_DEGREES = 2.5f;
    private static final Random RANDOM = new Random();

    private int phase = IDLE;
    private float spinYaw;
    private int freezeLeft, introLeft, boltId;
    private long nextHitAt, nextBoltAt, shakeUntil;
    private Player lastTarget;
    private boolean deathSpoken;

    public ImStorkingIt() {
        super("Im Storking It", "Rise, spin, TP-aura strike nearest player", Category.COMBAT);
        addCombatSafetyToggles();
    }

    @Override
    public void onEnable() {
        Minecraft mc = mc();
        LocalPlayer player = mc != null ? mc.player : null;
        if (player == null) { setEnabled(false); return; }
        phase = INTRO;
        introLeft = introTicks.getInt();
        spinYaw = player.getYRot();
        freezeLeft = 0;
        nextHitAt = nextBoltAt = shakeUntil = 0L;
        lastTarget = null;
        deathSpoken = false;
        TimerHooks.reset();
        StorkFx.startSplash();
        StorkFx.setGunActive(false);
    }

    @Override
    public void onDisable() {
        TimerHooks.reset();
        StorkFx.setGunActive(false);
        StorkFx.endSplash();
        phase = IDLE;
        freezeLeft = introLeft = 0;
        nextHitAt = shakeUntil = 0L;
        lastTarget = null;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || !mc.player.isAlive()) {
            if (isEnabled()) setEnabled(false);
            return;
        }
        LocalPlayer player = mc.player;
        if (phase == IDLE) return;

        double h = heightAboveGround(mc, player);
        double targetH = height.get();

        switch (phase) {
            case INTRO -> {
                hover(player);
                if (--introLeft <= 0) phase = RISING;
            }
            case RISING -> {
                float progress = (float) Mth.clamp(h / targetH, 0.0, 1.0);
                spin(player, Mth.lerp(progress, MIN_SPIN, MAX_SPIN));
                player.setDeltaMovement(0.0, riseSpeed.get(), 0.0);
                player.fallDistance = 0.0f;
                if (h >= targetH) {
                    phase = FROZEN;
                    freezeLeft = Math.max(1, freezeTicks.getInt());
                }
            }
            case FROZEN -> {
                holdHeight(player, h, targetH);
                spin(player, MAX_SPIN);
                if (--freezeLeft <= 0) {
                    phase = STRIKING;
                    StorkFx.setGunActive(true);
                    TimerHooks.set(timerSpeed.getFloat());
                }
            }
            case STRIKING -> {
                StorkFx.setGunActive(true);
                TimerHooks.set(timerSpeed.getFloat());
                holdHeight(player, h, targetH);
                if (!deathSpoken && lastTarget != null && !lastTarget.isAlive()) {
                    StorkFx.speakDeath();
                    deathSpoken = true;
                    lastTarget = null;
                }
                Player target = nearestPlayer(mc, player);
                if (target == null) {
                    spin(player, MAX_SPIN);
                    return;
                }
                lastTarget = target;
                aim(player, target);
                long now = System.nanoTime();
                float charge = 0.0f;
                try { charge = player.getAttackStrengthScale(0.5f); } catch (Throwable ignored) {}
                if (now >= nextHitAt && charge >= minCharge.getFloat()) {
                    nextHitAt = now + (long) (1_000_000_000L / Math.max(0.5, cps.get()));
                    teleportHit(mc, player, target);
                }
            }
            default -> {}
        }
    }

    private void teleportHit(Minecraft mc, LocalPlayer player, Player target) {
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || mc.gameMode == null) return;

        Vec3 home = player.position();
        Vec3 toHome = home.subtract(target.position());
        Vec3 flat = new Vec3(toHome.x, 0.0, toHome.z);
        double sd = strikeDist.get();
        Vec3 offset = flat.lengthSqr() < 1.0E-4 ? new Vec3(sd, 0.0, 0.0) : flat.normalize().scale(sd);
        Vec3 strike = target.position().add(offset);

        hop(connection, player, home, strike);
        try { mc.gameMode.attack(player, target); } catch (Throwable ignored) {}
        try { player.swing(InteractionHand.MAIN_HAND); } catch (Throwable ignored) {}
        hop(connection, player, strike, home);

        shakeUntil = System.nanoTime() + SHAKE_NANOS;
        if (visualBolt.get()) strikeBolt(mc, target);
        SilentRotate.markTarget(target);
        StorkFx.onHit();
    }

    private void hop(ClientPacketListener connection, LocalPlayer player, Vec3 from, Vec3 to) {
        Vec3 path = to.subtract(from);
        double hop = hopSize.get();
        int steps = Math.max(1, (int) Math.ceil(path.length() / hop));
        boolean horiz = false;
        try { horiz = player.horizontalCollision; } catch (Throwable ignored) {}
        for (int i = 1; i <= steps; i++) {
            Vec3 at = from.add(path.scale(i / (double) steps));
            sendPos(connection, at.x, at.y, at.z, false, horiz);
        }
    }

    private static void sendPos(ClientPacketListener connection, double x, double y, double z,
                                boolean onGround, boolean horiz) {
        try {
            connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, onGround, horiz));
            return;
        } catch (Throwable ignored) {}
        try {
            connection.send(new ServerboundMovePlayerPacket.Pos(new Vec3(x, y, z), onGround, horiz));
            return;
        } catch (Throwable ignored) {}
        try {
            Class<?> posCl = Class.forName("net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$Pos");
            for (var c : posCl.getConstructors()) {
                Class<?>[] pt = c.getParameterTypes();
                Object pkt = null;
                try {
                    if (pt.length == 5 && pt[0] == double.class) pkt = c.newInstance(x, y, z, onGround, horiz);
                    else if (pt.length == 3 && Vec3.class.isAssignableFrom(pt[0])) pkt = c.newInstance(new Vec3(x, y, z), onGround, horiz);
                    else if (pt.length == 4 && pt[0] == double.class) pkt = c.newInstance(x, y, z, onGround);
                } catch (Throwable ignored2) {}
                if (pkt != null) {
                    connection.send((net.minecraft.network.protocol.Packet<?>) pkt);
                    return;
                }
            }
        } catch (Throwable ignored) {}
    }

    private void strikeBolt(Minecraft mc, Player target) {
        long now = System.nanoTime();
        if (now < nextBoltAt || mc.level == null || target == null) return;
        nextBoltAt = now + BOLT_EVERY_NANOS;

        double tx = target.getX(), ty = target.getY(), tz = target.getZ();

        try {
            mc.level.playLocalSound(tx, ty, tz,
                    net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
                    net.minecraft.sounds.SoundSource.WEATHER,
                    8.0f, 0.9f + RANDOM.nextFloat() * 0.2f, false);
        } catch (Throwable t) {
            try {
                Object ev = Class.forName("net.minecraft.sounds.SoundEvents").getField("LIGHTNING_BOLT_THUNDER").get(null);
                Object src = Class.forName("net.minecraft.sounds.SoundSource").getField("WEATHER").get(null);
                mc.level.getClass().getMethod("playLocalSound", double.class, double.class, double.class,
                                Class.forName("net.minecraft.sounds.SoundEvent"),
                                Class.forName("net.minecraft.sounds.SoundSource"),
                                float.class, float.class, boolean.class)
                        .invoke(mc.level, tx, ty, tz, ev, src, 8.0f, 1.0f, false);
            } catch (Throwable ignored) {}
        }
        try {
            mc.level.playLocalSound(tx, ty, tz,
                    net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT,
                    net.minecraft.sounds.SoundSource.WEATHER,
                    2.0f, 1.0f, false);
        } catch (Throwable ignored) {}

        try {
            var level = mc.level;
            for (int i = 0; i < 18; i++) {
                double ox = (RANDOM.nextDouble() - 0.5) * 2.2;
                double oy = RANDOM.nextDouble() * 2.4;
                double oz = (RANDOM.nextDouble() - 0.5) * 2.2;
                try {
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                            tx + ox * 0.6, ty + oy, tz + oz * 0.6,
                            (RANDOM.nextDouble() - 0.5) * 0.4,
                            RANDOM.nextDouble() * 0.3,
                            (RANDOM.nextDouble() - 0.5) * 0.4);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            Object type = resolveLightningType();
            if (type == null) return;
            Object bolt = null;
            try {
                Class<?> reason = Class.forName("net.minecraft.world.entity.EntitySpawnReason");
                Object triggered = reason.getField("TRIGGERED").get(null);
                bolt = type.getClass().getMethod("create", net.minecraft.world.level.Level.class, reason)
                        .invoke(type, mc.level, triggered);
            } catch (Throwable t1) {
                try {
                    bolt = type.getClass().getMethod("create", net.minecraft.world.level.Level.class)
                            .invoke(type, mc.level);
                } catch (Throwable ignored) {}
            }
            if (bolt == null) return;
            try { bolt.getClass().getMethod("setVisualOnly", boolean.class).invoke(bolt, true); } catch (Throwable ignored) {}
            try { bolt.getClass().getMethod("setId", int.class).invoke(bolt, -1_000_000 - (boltId++ & 0xFFFF)); } catch (Throwable ignored) {}
            try {
                bolt.getClass().getMethod("snapTo", double.class, double.class, double.class, float.class, float.class)
                        .invoke(bolt, tx, ty, tz, 0f, 0f);
            } catch (Throwable t) {
                try { bolt.getClass().getMethod("setPos", double.class, double.class, double.class).invoke(bolt, tx, ty, tz); } catch (Throwable ignored) {}
            }
            try {
                mc.level.getClass().getMethod("addEntity", net.minecraft.world.entity.Entity.class).invoke(mc.level, bolt);
            } catch (Throwable t2) {
                try {
                    mc.level.getClass().getMethod("addFreshEntity", net.minecraft.world.entity.Entity.class).invoke(mc.level, bolt);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private static Object resolveLightningType() {
        try {
            Class<?> types = Class.forName("net.minecraft.world.entity.EntityType");
            try {
                Object f = types.getField("LIGHTNING_BOLT").get(null);
                if (f != null) {
                    try { return f.getClass().getMethod("value").invoke(f); } catch (Throwable ignored) {}
                    return f;
                }
            } catch (Throwable ignored) {}
            try {
                Class<?> reg = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
                Object entityType = reg.getField("ENTITY_TYPE").get(null);
                Object id = Class.forName("net.minecraft.resources.Identifier")
                        .getMethod("parse", String.class).invoke(null, "minecraft:lightning_bolt");
                Object got = entityType.getClass().getMethod("get", Object.class).invoke(entityType, id);
                if (got != null) {
                    try { return got.getClass().getMethod("value").invoke(got); } catch (Throwable ignored) {}
                    return got;
                }
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
        return null;
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor graphics, float partialTick) {
        Minecraft mc = mc();
        if (mc == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        StorkFx.render(graphics, mc.font, sw, sh);
    }

    public float shakeYaw() { return shake(); }
    public float shakePitch() { return shake() * 0.6f; }

    private float shake() {
        long left = shakeUntil - System.nanoTime();
        if (left <= 0) return 0.0f;
        float strength = SHAKE_DEGREES * left / (float) SHAKE_NANOS;
        return (RANDOM.nextFloat() * 2.0f - 1.0f) * strength;
    }

    private double heightAboveGround(Minecraft client, LocalPlayer player) {
        BlockPos.MutableBlockPos pos = player.blockPosition().mutable();
        int floor = client.level.getMinY();
        for (int i = 0; i < 128 && pos.getY() > floor; i++) {
            pos.move(0, -1, 0);
            try {
                if (!client.level.getBlockState(pos).getCollisionShape(client.level, pos).isEmpty()) {
                    return player.getY() - (pos.getY() + 1);
                }
            } catch (Throwable t) { break; }
        }
        return 128.0;
    }

    private void holdHeight(LocalPlayer player, double h, double targetH) {
        player.fallDistance = 0.0f;
        player.setDeltaMovement(0.0, h < targetH - 1.0 ? riseSpeed.get() : 0.0, 0.0);
    }

    private void hover(LocalPlayer player) {
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0f;
    }

    private void spin(LocalPlayer player, float degreesPerTick) {
        spinYaw = Mth.wrapDegrees(spinYaw + degreesPerTick);
        if (silentSpin.get()) SilentRotate.lookSilent(player, spinYaw, 0.0f);
        else { player.setYRot(spinYaw); player.setXRot(0.0f); }
    }

    private void aim(LocalPlayer player, Player target) {
        float[] a = SilentRotate.anglesTo(player, target);
        spinYaw = a[0];
        if (silentSpin.get()) SilentRotate.lookSilent(player, a[0], a[1]);
        else { player.setYRot(a[0]); player.setXRot(Mth.clamp(a[1], -89.9f, 89.9f)); }
    }

    private Player nearestPlayer(Minecraft client, LocalPlayer self) {
        double r = range.get();
        double r2 = r * r;
        return client.level.players().stream()
                .filter(other -> other != self && other.isAlive() && !other.isSpectator())
                .filter(other -> {
                    if (ignoreFriends.get()) {
                        String name = playerName(other);
                        if (name != null && FriendsManager.isFriend(name)) return false;
                    }
                    return self.distanceToSqr(other) <= r2;
                })
                .min(Comparator.comparingDouble(self::distanceToSqr))
                .orElse(null);
    }

    private static String playerName(Player p) {
        if (p == null) return null;
        try {
            var profile = p.getGameProfile();
            if (profile == null) return null;
            try { return (String) profile.getClass().getMethod("getName").invoke(profile); }
            catch (Throwable t1) {
                try { return (String) profile.getClass().getMethod("name").invoke(profile); }
                catch (Throwable t2) {
                    try {
                        Object name = profile.getClass().getMethod("name").invoke(profile);
                        if (name != null) return name.toString();
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
        try { return p.getName().getString(); } catch (Throwable ignored) {}
        return null;
    }
}
