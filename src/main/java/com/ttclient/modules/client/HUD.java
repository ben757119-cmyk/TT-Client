package com.ttclient.modules.client;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ColorSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class HUD extends Module {
    public final BoolSetting watermark = addSetting(new BoolSetting("Watermark", "Show TT Client watermark", true));
    public final BoolSetting coords = addSetting(new BoolSetting("Coordinates", "Show coords", true));
    public final BoolSetting fps = addSetting(new BoolSetting("FPS", "Show FPS", true));
    public final BoolSetting direction = addSetting(new BoolSetting("Direction", "Show facing", true));
    public final BoolSetting armor = addSetting(new BoolSetting("Armor", "Show armor status", true));
    public final BoolSetting arrayList = addSetting(new BoolSetting("ArrayList", "Show enabled modules", true));
    public final ColorSetting color = addSetting(new ColorSetting("Color", "HUD color", 0xFF00FFAA));

    public HUD() {
        super("HUD", "Customizable heads-up display", Category.CLIENT);
        setEnabled(true);
    }

    @Override
    public void onRender2D(GuiGraphics g, float partialTick) {
        if (mc.player == null || mc.options.hideGui) return;
        Font font = mc.font;
        int col = color.get();
        int x = 4, y = 4;
        if (watermark.get()) {
            g.drawString(font, "TT Client \u00a77v1.0.0", x + 1, y + 1, 0x000000, false);
            g.drawString(font, "TT Client \u00a77v1.0.0", x, y, col, false);
            y += 12;
        }
        if (fps.get()) {
            String text = mc.getFps() + " FPS";
            g.drawString(font, text, x + 1, y + 1, 0x000000, false);
            g.drawString(font, text, x, y, col, false);
            y += 12;
        }
        if (coords.get()) {
            String text = String.format("XYZ \u00a7f%.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            g.drawString(font, text, x + 1, y + 1, 0x000000, false);
            g.drawString(font, text, x, y, col, false);
            y += 12;
        }
        if (direction.get()) {
            float yaw = Mth.wrapDegrees(mc.player.getYRot());
            String dir = yaw >= -45 && yaw < 45 ? "South" : yaw >= 45 && yaw < 135 ? "West" : yaw >= 135 || yaw < -135 ? "North" : "East";
            String text = String.format("Facing \u00a7f%s (%.0f)", dir, yaw);
            g.drawString(font, text, x + 1, y + 1, 0x000000, false);
            g.drawString(font, text, x, y, col, false);
            y += 12;
        }
        if (arrayList.get()) {
            int ay = 4;
            int screenW = mc.getWindow().getGuiScaledWidth();
            for (Module m : com.ttclient.TTClient.modules.getModules()) {
                if (m.isEnabled() && !(m instanceof ClickGUIModule) && !(m instanceof HUD)) {
                    String name = m.getName();
                    int w = font.width(name);
                    g.drawString(font, name, screenW - w - 5 + 1, ay + 1, 0x000000, false);
                    g.drawString(font, name, screenW - w - 5, ay, col, false);
                    ay += 11;
                }
            }
        }
    }
}
