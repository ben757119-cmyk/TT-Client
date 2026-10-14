package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.modules.player.Freecam;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Soft anti "Flying is not enabled on this server" kick.
 * Spoofs onGround / clears fall distance and disables creative-fly flags
 * so movement packets look less like unauthorized flight.
 *
 * Not a full packet-level AC bypass — strict anti-cheats can still flag velocity.
 */
public class AntiFlightKick extends Module {
    public final BoolSetting alwaysGround = addSetting(new BoolSetting(
            "GroundSpoof", "Report onGround=true while airborne", true));
    public final BoolSetting stripAbilities = addSetting(new BoolSetting(
            "StripFlyFlag", "Force abilities.flying=false (recommended)", true));
    public final NumberSetting groundInterval = addSetting(new NumberSetting(
            "SpoofEvery", "Ticks between hard ground spoofs (1=every tick)", 1, 1, 10, 1));

    private int tick;

    public AntiFlightKick() {
        super("AntiFlightKick",
                "Reduce vanilla flight kicks while using Fly / similar",
                Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        LocalPlayer p = mc.player;

        if (p.isCreative() || p.isSpectator()) return;

        boolean floating = isFloatingModuleActive() || !p.onGround();

        // Always clear fall damage accumulation while this module is on and airborne
        if (!p.onGround() || isFloatingModuleActive()) {
            p.fallDistance = 0;
        }

        if (stripAbilities.get()) {
            // Creative fly bit is what triggers the vanilla kick message fastest
            p.getAbilities().flying = false;
            if (!p.isCreative()) {
                p.getAbilities().mayfly = false;
            }
        }

        if (alwaysGround.get() && (isFloatingModuleActive() || floating)) {
            tick++;
            int every = Math.max(1, groundInterval.getInt());
            if (tick % every == 0) {
                p.setOnGround(true);
            }
        }
    }

    private boolean isFloatingModuleActive() {
        if (TTClientModules() == null) return false;
        var mgr = TTClientModules();
        Module fly = mgr.getModule(Fly.class);
        if (fly != null && fly.isEnabled()) return true;
        Module boat = mgr.getModule(BoatFly.class);
        if (boat != null && boat.isEnabled()) return true;
        Module elytra = mgr.getModule(ElytraFly.class);
        if (elytra != null && elytra.isEnabled()) return true;
        Module freecam = mgr.getModule(Freecam.class);
        if (freecam != null && freecam.isEnabled()) return true;
        Module noclip = mgr.getModule(NoClip.class);
        return noclip != null && noclip.isEnabled();
    }

    private static com.ttclient.modules.ModuleManager TTClientModules() {
        return com.ttclient.client.TTClientClient.modules;
    }
}
