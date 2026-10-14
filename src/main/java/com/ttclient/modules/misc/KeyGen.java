package com.ttclient.modules.misc;

import com.ttclient.client.NotificationManager;
import com.ttclient.license.LicenseManager;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.ModeSetting;
import net.minecraft.client.Minecraft;

/**
 * Master-only: generate single-use timed license keys for beta builds.
 * Toggle ON to mint a key (printed to chat + notification).
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
        Minecraft mc = mc();
        if (mc != null && mc.player != null) {
            try {
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "[TT] Beta key (" + duration.get() + ", one-time):\n" + key),
                        false);
            } catch (Throwable t) {
                try {
                    mc.gui.getChat().addMessage(
                            net.minecraft.network.chat.Component.literal("[TT] Key: " + key));
                } catch (Throwable ignored) {}
            }
        }
        NotificationManager.push("KeyGen", duration.get() + " key copied to chat");
        TTClientLog(key);
        setEnabled(false);
    }

    private static void TTClientLog(String key) {
        try {
            com.ttclient.TTClient.LOGGER.info("Generated license key: {}", key);
        } catch (Throwable ignored) {}
    }
}
