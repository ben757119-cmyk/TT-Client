package com.ttclient.modules.misc;

import com.ttclient.client.NotificationManager;
import com.ttclient.license.LicenseManager;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Beta license status + redeem via chat.
 * Type in chat:  TTKEY <yourkey>
 */
public class LicenseModule extends Module {
    public LicenseModule() {
        super("License",
                "Beta license status. Redeem: type TTKEY <key> in chat",
                Category.CLIENT);
        setEnabled(true);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        Minecraft mc = mc();
        if (mc == null) return;
        String line = "TT " + LicenseManager.statusText();
        g.text(mc.font, line, 4, 4, LicenseManager.isUnlocked() ? 0xFF00E8A0 : 0xFFFF5555, false);
    }

    /** Parse chat for TTKEY redeem commands (local outbound). */
    public static void tryRedeemFromChat(String message) {
        if (message == null) return;
        String t = message.trim();
        if (!t.toUpperCase().startsWith("TTKEY ")) return;
        String key = t.substring(6).trim();
        String err = LicenseManager.redeem(key);
        if (err == null) {
            NotificationManager.push("License", "Key accepted");
        } else {
            NotificationManager.push("License", err);
        }
    }
}
