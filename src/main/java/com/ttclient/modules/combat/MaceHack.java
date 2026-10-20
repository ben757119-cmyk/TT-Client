package com.ttclient.modules.combat;

import com.ttclient.client.TTClientClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.util.InvUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Wurst MaceDMG port for NeoForge 26.2.
 *
 * Sequence (before attack packet):
 *   4× Y+0 onGround=false, 1× Y+sqrt(500), 1× Y+0
 */
public class MaceHack extends Module {
    public final BoolSetting onlyPlayers = addSetting(new BoolSetting(
            "OnlyPlayers", "Only when targeting players", false));

    public MaceHack() {
        super("MaceHack", "Mace smash on ground (Wurst packet spoof)", Category.COMBAT);
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

    /** Called from mixin / AttackEntityEvent right before interact packet. */
    public static void onPlayerAttack(Player attacker, Entity target) {
        try {
            if (attacker == null || target == null) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            if (attacker != mc.player) return;

            var mods = TTClientClient.modules;
            if (mods == null) return;
            MaceHack h = mods.getModule(MaceHack.class);
            if (h == null || !h.isEnabled()) return;

            if (!holdingMace(mc.player)) return;
            if (h.onlyPlayers.get() && !(target instanceof Player)) return;

            sendWurstFallSpoof(mc.player);
        } catch (Throwable ignored) {}
    }

    public static void spoofBeforeAttack(LocalPlayer p) {
        if (p == null) return;
        if (!isActive()) return;
        if (!holdingMace(p)) return;
        sendWurstFallSpoof(p);
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

    private static void sendWurstFallSpoof(LocalPlayer p) {
        if (p == null || p.connection == null) return;
        double x = p.getX();
        double y = p.getY();
        double z = p.getZ();
        boolean horiz = false;
        try { horiz = p.horizontalCollision; } catch (Throwable ignored) {}

        double peak = Math.sqrt(500.0);

        for (int i = 0; i < 4; i++) {
            sendPos(p, x, y, z, false, horiz);
        }
        sendPos(p, x, y + peak, z, false, horiz);
        sendPos(p, x, y, z, false, horiz);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void sendPos(LocalPlayer p, double x, double y, double z, boolean onGround, boolean horiz) {
        Packet pkt = null;

        // Prefer 5-arg Pos(x,y,z,onGround,horizontalCollision) — matches Wurst / 1.21+
        try {
            pkt = new ServerboundMovePlayerPacket.Pos(x, y, z, onGround, horiz);
        } catch (Throwable ignored) {
            pkt = null;
        }

        if (pkt == null) {
            try {
                pkt = new ServerboundMovePlayerPacket.Pos(new Vec3(x, y, z), onGround, horiz);
            } catch (Throwable ignored) {
                pkt = null;
            }
        }

        if (pkt == null) {
            try {
                Class<?> posCl = Class.forName("net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$Pos");
                for (var c : posCl.getConstructors()) {
                    Class<?>[] pt = c.getParameterTypes();
                    Object built = null;
                    try {
                        if (pt.length == 5 && pt[0] == double.class) {
                            built = c.newInstance(x, y, z, onGround, horiz);
                        } else if (pt.length == 3 && Vec3.class.isAssignableFrom(pt[0])) {
                            built = c.newInstance(new Vec3(x, y, z), onGround, horiz);
                        } else if (pt.length == 4 && pt[0] == double.class) {
                            built = c.newInstance(x, y, z, onGround);
                        }
                    } catch (Throwable ignored) {}
                    if (built instanceof Packet) {
                        pkt = (Packet) built;
                        break;
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (pkt != null) {
            try {
                p.connection.send(pkt);
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void onTick() {}
}
