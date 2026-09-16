package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;

public class CoordCopy extends Module {
    public CoordCopy() {
        super("CoordCopy", "Copy coordinates to clipboard", Category.MISC);
    }

    @Override
    public void onEnable() {
        Minecraft mc = mc();
        if (mc != null && mc.player != null) {
            String text = String.format("%.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            mc.keyboardHandler.setClipboard(text);
        }
        setEnabled(false);
    }
}
