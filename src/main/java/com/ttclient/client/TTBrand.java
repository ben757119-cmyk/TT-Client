package com.ttclient.client;

import com.ttclient.TTClient;
import com.ttclient.modules.client.ClickGUIModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/** Shared TT Client chrome drawn on menus / loading / inventory. */
public final class TTBrand {
    private TTBrand() {}

    public static int accent() {
        return ClickGUIModule.globalAccent();
    }

    public static boolean shouldBrand(Screen s) {
        if (s == null) return false;
        String n = s.getClass().getName().toLowerCase();
        if (n.contains("clickgui") || n.contains("tttitle")) return false;
        return n.contains("multiplayer") || n.contains("joinmulti")
                || n.contains("server") || n.contains("connect")
                || n.contains("progress") || n.contains("loading")
                || n.contains("receiving") || n.contains("download")
                || n.contains("level") && n.contains("load")
                || n.contains("inventory") || n.contains("container")
                || n.contains("creative") || n.contains("pause")
                || n.contains("options") || n.contains("worldselection")
                || n.contains("selectworld") || n.contains("genericdirt")
                || n.contains("pack") || n.contains("stats");
    }

    public static String brandLabel(Screen s) {
        String n = s.getClass().getName().toLowerCase();
        if (n.contains("connect") || n.contains("receiving")) return "TT Client  ·  connecting";
        if (n.contains("progress") || n.contains("loading") || n.contains("download"))
            return "TT Client  ·  loading";
        if (n.contains("inventory") || n.contains("container") || n.contains("creative"))
            return "TT Client";
        if (n.contains("multiplayer") || n.contains("server")) return "TT Client  v" + TTClient.VERSION;
        return "TT Client  v" + TTClient.VERSION;
    }

    public static void drawWatermark(GuiGraphicsExtractor g, Screen s) {
        if (g == null || s == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return;
        int ac = accent();
        String mark = brandLabel(s);
        int tw = mc.font.width(mark);
        int x = 8;
        int y = Math.max(8, s.height - 16);
        g.fill(x - 2, y - 3, x + tw + 8, y + 11, 0xC0080A0E);
        g.fill(x - 2, y - 3, x, y + 11, ac);
        g.text(mc.font, mark, x + 2, y, ac, false);
    }

    public static void drawTopBar(GuiGraphicsExtractor g, Screen s) {
        if (g == null || s == null) return;
        int ac = accent();
        g.fill(0, 0, s.width, 2, ac);
        g.fill(0, 2, s.width, 3, (ac & 0x00FFFFFF) | 0x44000000);
    }
}
