package com.ttclient.modules.client;

import com.ttclient.client.TTClientClient;
import com.ttclient.gui.ClickGUIScreen;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * When enabled, TT Client draws nothing: no HUD, no notifications, no ClickGUI.
 * Modules still run in the background — only visuals are suppressed so OBS/Discord
 * game capture will not show the client overlay.
 */
public class StreamerMode extends Module {
    public final BoolSetting blockGui = addSetting(new BoolSetting(
            "BlockClickGUI", "Prevent opening ClickGUI while active", true));

    public StreamerMode() {
        super("StreamerMode",
                "Hide all TT Client visuals from screen capture (OBS)",
                Category.CLIENT);
    }

    public static boolean active() {
        if (TTClientClient.modules == null) return false;
        StreamerMode m = TTClientClient.modules.getModule(StreamerMode.class);
        return m != null && m.isEnabled();
    }

    @Override
    public void onEnable() {
        // Close GUI immediately so it never sits on stream
        Minecraft mc = mc();
        if (mc == null) return;
        try {
            Screen s = mc.gui.screen();
            if (s instanceof ClickGUIScreen) {
                mc.gui.setScreen(null);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void onTick() {
        if (!blockGui.get()) return;
        Minecraft mc = mc();
        if (mc == null) return;
        try {
            Screen s = mc.gui.screen();
            if (s instanceof ClickGUIScreen) {
                mc.gui.setScreen(null);
            }
        } catch (Throwable ignored) {}
    }
}
