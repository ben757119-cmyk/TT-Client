package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.render.EspDraw;
import com.ttclient.render.EspProjection;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Meteor-style enhanced nametags (screen-space).
 */
public class NameTags extends Module {
    public final BoolSetting health = addSetting(new BoolSetting("Health", "Show health", true));
    public final BoolSetting distance = addSetting(new BoolSetting("Distance", "Show distance", true));
    public final BoolSetting self = addSetting(new BoolSetting("Self", "Show on yourself", false));
    public final NumberSetting scale = addSetting(new NumberSetting("Scale", "Reserved", 1.0, 0.5, 2.0, 0.1));

    public NameTags() {
        super("NameTags", "Meteor-style player nametags", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.level == null || mc.player == null || mc.font == null) return;

        double[] screen = new double[2];
        for (Player p : mc.level.players()) {
            if (!(p instanceof AbstractClientPlayer)) continue;
            if (!self.get() && p == mc.player) continue;
            if (p.isInvisible()) continue;

            double x = p.getX();
            double y = p.getY() + p.getBbHeight() + 0.35;
            double z = p.getZ();
            if (!EspProjection.worldToScreen(x, y, z, screen)) continue;

            StringBuilder sb = new StringBuilder(p.getName().getString());
            if (health.get()) {
                float hp = p.getHealth() + p.getAbsorptionAmount();
                sb.append(" ").append(String.format("§c%.0f❤", hp));
            }
            if (distance.get()) {
                double d = mc.player.distanceTo(p);
                sb.append(" ").append(String.format("§7[%.0fm]", d));
            }

            String text = sb.toString().replace('§', '§');
            // Strip section signs for width; draw plain then color roughly
            String plain = text.replaceAll("\u00A7.", "");
            EspDraw.label(g, mc.font, plain, screen[0], screen[1], 0xFFFFFFFF);
        }
    }
}
