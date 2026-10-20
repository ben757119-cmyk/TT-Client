package com.ttclient.modules.combat;

import com.ttclient.client.TTClientClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.util.InvUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Wurst MaceDMG-style fall spoof for NeoForge 26.2.
 *
 * On attack-key edge (ClientTick Pre), send:
 *   4× Y+0 onGround=false, 1× Y+sqrt(500), 1× Y+0
 * so the following vanilla attack sees server fallDistance.
 */
public class MaceHack extends Module {
    public final BoolSetting onlyPlayers = addSetting(new BoolSetting(
            "OnlyPlayers", "Only when crosshair is a player", false));

    private boolean wasAttack;

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

    public static void onPlayerAttack(Player attacker, Entity target) {
        try {
            if (attacker == null) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null || attacker != mc.player) return;
            if (!isActive()) return;
            if (!holdingMace(mc.player)) return;
            sendWurstFallSpoof(mc.player);
        } catch (Throwable ignored) {}
    }

    public static void spoofBeforeAttack(LocalPlayer p) {
        if (p == null || !isActive() || !holdingMace(p)) return;
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

    /** Call from ClientTick Pre so packets leave before vanilla attack this tick. */
    public void onPreTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null) return;
        if (!holdingMace(mc.player)) {
            wasAttack = false;
            return;
        }

        boolean down = false;
        try { down = mc.options.keyAttack.isDown(); } catch (Throwable ignored) {}
        boolean edge = down && !wasAttack;
        wasAttack = down;
        if (!edge) return;

        if (onlyPlayers.get()) {
            try {
                HitResult hr = mc.hitResult;
                if (!(hr instanceof EntityHitResult ehr) || !(ehr.getEntity() instanceof Player)) return;
            } catch (Throwable t) {
                return;
            }
        }

        sendWurstFallSpoof(mc.player);
    }

    private static void sendWurstFallSpoof(LocalPlayer p) {
        if (p == null || p.connection == null) return;
        double x = p.getX();
        double y = p.getY();
        double z = p.getZ();
        boolean horiz = false;
        try { horiz = p.horizontalCollision; } catch (Throwable ignored) {}

        double peak = Math.sqrt(500.0);
        for (int i = 0; i < 4; i++) sendPos(p, x, y, z, false, horiz);
        sendPos(p, x, y + peak, z, false, horiz);
        sendPos(p, x, y, z, false, horiz);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void sendPos(LocalPlayer p, double x, double y, double z, boolean onGround, boolean horiz) {
        try {
            Class<?> posCl = Class.forName("net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$Pos");
            Object built = null;
            for (var c : posCl.getConstructors()) {
                Class<?>[] pt = c.getParameterTypes();
                try {
                    if (pt.length == 5 && pt[0] == double.class) {
                        built = c.newInstance(x, y, z, onGround, horiz);
                        break;
                    }
                } catch (Throwable ignored) {}
            }
            if (built == null) {
                for (var c : posCl.getConstructors()) {
                    Class<?>[] pt = c.getParameterTypes();
                    try {
                        if (pt.length == 3 && Vec3.class.isAssignableFrom(pt[0])) {
                            built = c.newInstance(new Vec3(x, y, z), onGround, horiz);
                            break;
                        }
                    } catch (Throwable ignored) {}
                }
            }
            if (built == null) {
                for (var c : posCl.getConstructors()) {
                    Class<?>[] pt = c.getParameterTypes();
                    try {
                        if (pt.length == 4 && pt[0] == double.class) {
                            built = c.newInstance(x, y, z, onGround);
                            break;
                        }
                    } catch (Throwable ignored) {}
                }
            }
            if (built instanceof Packet) {
                p.connection.send((Packet) built);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void onDisable() {
        wasAttack = false;
    }

    @Override
    public void onTick() {
        // attack edge handled in onPreTick via TTClientClient
    }
}
