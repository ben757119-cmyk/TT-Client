package com.ttclient.modules.world;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class Waypoints extends Module {
    public final BoolSetting autoDeath = addSetting(new BoolSetting("DeathPoint", "Save last death location", true));
    private final List<String> points = new ArrayList<>();
    private BlockPos lastDeath;

    public Waypoints() {
        super("Waypoints", "Track important coordinates on the HUD", Category.WORLD);
        setEnabled(true);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (autoDeath.get() && mc.player.getHealth() <= 0 && lastDeath == null) {
            lastDeath = mc.player.blockPosition();
            String label = "Death " + lastDeath.getX() + " " + lastDeath.getY() + " " + lastDeath.getZ();
            if (!points.contains(label)) points.add(label);
        }
        if (mc.player.getHealth() > 0) lastDeath = null;
    }

    @Override
    public void onRender2D(GuiGraphics g, float partialTick) {
        if (mc.player == null || mc.options.hideGui || points.isEmpty()) return;
        int y = mc.getWindow().getGuiScaledHeight() / 2;
        g.drawString(mc.font, "Waypoints", 4, y, 0xFF00FFAA, false);
        y += 12;
        for (String p : points) {
            g.drawString(mc.font, p, 4, y, 0xFFCCCCCC, false);
            y += 11;
        }
    }
}
