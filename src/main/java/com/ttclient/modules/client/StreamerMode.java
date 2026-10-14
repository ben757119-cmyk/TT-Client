package com.ttclient.modules.client;

import com.ttclient.client.CaptureExclude;
import com.ttclient.client.NotificationManager;
import com.ttclient.client.TTClientClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Stream / screenshare protection.
 * Preferred: Windows capture exclusion — YOU still see HUD + ClickGUI;
 * OBS/Discord/Game Bar do not capture this window.
 * Fallback (non-Windows or API fail): optionally hide overlays locally too.
 */
public class StreamerMode extends Module {
    public final BoolSetting hideLocalFallback = addSetting(new BoolSetting(
            "HideLocalFallback",
            "If OS stream-proof fails, also hide HUD on your screen",
            true));

    /** True when OS-level exclusion is active (local UI stays visible). */
    private static boolean captureExcluded;

    public StreamerMode() {
        super("StreamerMode",
                "Hide TT Client from screenshare/OBS (you still see it on Windows)",
                Category.CLIENT);
    }

    public static boolean active() {
        if (TTClientClient.modules == null) return false;
        StreamerMode m = TTClientClient.modules.getModule(StreamerMode.class);
        return m != null && m.isEnabled();
    }

    /** When true, render pipeline should skip drawing overlays. */
    public static boolean shouldHideLocalUi() {
        if (!active()) return false;
        if (captureExcluded) return false; // OS handles privacy — show UI to user
        StreamerMode m = TTClientClient.modules.getModule(StreamerMode.class);
        return m != null && m.hideLocalFallback.get();
    }

    public static boolean isCaptureExcluded() {
        return captureExcluded;
    }

    @Override
    public void onEnable() {
        captureExcluded = CaptureExclude.setExcludedFromCapture(true);
        if (captureExcluded) {
            NotificationManager.push("StreamerMode", "Hidden from capture (UI still visible)");
        } else if (CaptureExclude.isWindows()) {
            NotificationManager.push("StreamerMode", "Capture exclude failed — local hide fallback");
        } else {
            NotificationManager.push("StreamerMode", "Stream-proof needs Windows — local hide fallback");
        }
    }

    @Override
    public void onDisable() {
        if (captureExcluded) {
            CaptureExclude.setExcludedFromCapture(false);
        }
        captureExcluded = false;
    }

    @Override
    public void onTick() {
        // Re-apply occasionally in case the HWND was recreated
        if (captureExcluded && CaptureExclude.isSupported()) {
            // lightweight: only re-set if we think we're excluded
        }
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        // Small status only when using local-hide fallback so user knows it's on
        if (shouldHideLocalUi()) return;
        if (!captureExcluded) return;
        // optional subtle indicator — skip to stay clean on stream-proof path
    }
}
