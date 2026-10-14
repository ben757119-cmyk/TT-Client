package com.ttclient.modules.misc;

import com.ttclient.client.NotificationManager;
import com.ttclient.license.LicenseManager;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Master-only: generate single-use timed license keys for beta builds.
 * Toggle ON to mint a key (logged + notification; copy from latest.log if needed).
 */
public class KeyGen extends Module {
    public final ModeSetting duration = addSetting(new ModeSetting(
            "Duration", "How long the key lasts",
            "1 hour", "15 min", "1 hour", "6 hours", "1 day", "7 days"));

    public KeyGen() {
        super("KeyGen", "MASTER: generate a one-time beta license key", Category.CLIENT);
    }

    @Override
    public void onEnable() {
        if (!LicenseManager.isMaster()) {
            NotificationManager.push("KeyGen", "Master build only");
            setEnabled(false);
            return;
        }

        long ms = switch (duration.get()) {
            case "15 min" -> 15L * 60 * 1000;
            case "6 hours" -> 6L * 60 * 60 * 1000;
            case "1 day" -> 24L * 60 * 60 * 1000;
            case "7 days" -> 7L * 24 * 60 * 60 * 1000;
            default -> 60L * 60 * 1000;
        };

        String key = LicenseManager.generateKey(ms);
        com.ttclient.TTClient.LOGGER.info("[TT] Beta key ({} one-time): {}", duration.get(), key);
        NotificationManager.push("KeyGen", duration.get() + " key — see log");

        // Best-effort on-screen message (APIs vary by mapping)
        Minecraft mc = mc();
        if (mc != null) {
            Component msg = Component.literal("[TT] Key (" + duration.get() + "): " + key);
            try {
                mc.player.connection.sendChat("/tt key ignored");
            } catch (Throwable ignored) {}
            try {
                // Prefer system chat overlay if present
                var gui = mc.gui;
                var method = gui.getClass().getMethod("setOverlayMessage", Component.class, boolean.class);
                method.invoke(gui, msg, false);
            } catch (Throwable ignored) {}
            try {
                NotificationManager.push("TT Key", key.length() > 40 ? key.substring(0, 40) + "…" : key);
            } catch (Throwable ignored) {}
        }

        setEnabled(false);
    }
}
