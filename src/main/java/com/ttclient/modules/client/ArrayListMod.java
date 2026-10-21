package com.ttclient.modules.client;

import com.ttclient.TTClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Clean module list — top-right by default. */
public class ArrayListMod extends Module {
    public final BoolSetting rainbow = addSetting(new BoolSetting("Rainbow", "Rainbow colors", false));
    public final ModeSetting side = addSetting(new ModeSetting("Side", "Screen side", "Right", "Right", "Left"));
    public final BoolSetting background = addSetting(new BoolSetting("Background", "Panel behind each name", true));

    public ArrayListMod() {
        super("ArrayList", "Enabled modules list", Category.CLIENT);
        setEnabled(true);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.font == null || TTClient.modules == null) return;
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int ac = ClickGUIModule.globalAccent();

        List<Module> enabled = new ArrayList<>();
        for (Module m : TTClient.modules.getModules()) {
            if (!m.isEnabled()) continue;
            if (m instanceof ArrayListMod || m instanceof HUD || m instanceof Notifications) continue;
            if (m instanceof Keystrokes || m instanceof ClickGUIModule) continue;
            enabled.add(m);
        }
        enabled.sort(Comparator.comparingInt((Module m) -> -font.width(m.getName())));

        boolean left = "Left".equalsIgnoreCase(side.get());
        int y = 5;
        long time = System.currentTimeMillis();

        for (int i = 0; i < enabled.size(); i++) {
            Module m = enabled.get(i);
            String name = m.getName();
            int w = font.width(name);
            int color = ac;
            if (rainbow.get()) {
                float hue = ((time / 18f) + i * 18) % 360 / 360f;
                color = 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 0.55f, 1f);
            }

            int x = left ? 6 : sw - w - 12;
            if (background.get()) {
                if (left) {
                    g.fill(x - 4, y, x + w + 8, y + 12, 0xB0080A0E);
                    g.fill(x - 4, y, x - 2, y + 12, color);
                } else {
                    g.fill(x - 6, y, sw - 1, y + 12, 0xB0080A0E);
                    g.fill(sw - 2, y, sw, y + 12, color);
                }
            }
            g.text(font, name, x, y + 2, color, false);
            y += 13;
        }
    }
}
