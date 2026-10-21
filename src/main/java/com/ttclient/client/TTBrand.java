package com.ttclient.client;

import com.ttclient.TTClient;
import com.ttclient.modules.client.ClickGUIModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/**
 * Full-blast TT Client chrome on menus, loading, inventory, multiplayer.
 * Matches ClickGUI accent (incl. SeizureMode rainbow).
 */
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
                || (n.contains("level") && n.contains("load"))
                || n.contains("inventory") || n.contains("container")
                || n.contains("creative") || n.contains("pause")
                || n.contains("options") || n.contains("worldselection")
                || n.contains("selectworld") || n.contains("genericdirt")
                || n.contains("pack") || n.contains("stats")
                || n.contains("advancement") || n.contains("social")
                || n.contains("chatoptions") || n.contains("controls")
                || n.contains("accessibility") || n.contains("language")
                || n.contains("video") || n.contains("sound")
                || n.contains("death") || n.contains("gameover")
                || n.contains("confirm") || n.contains("disconnected")
                || n.contains("realms") || n.contains("share");
    }

    private static boolean isHeavy(Screen s) {
        String n = s.getClass().getName().toLowerCase();
        return n.contains("connect") || n.contains("progress") || n.contains("loading")
                || n.contains("receiving") || n.contains("download")
                || (n.contains("level") && n.contains("load"));
    }

    private static boolean isInventory(Screen s) {
        String n = s.getClass().getName().toLowerCase();
        return n.contains("inventory") || n.contains("container") || n.contains("creative");
    }

    private static boolean isMultiplayer(Screen s) {
        String n = s.getClass().getName().toLowerCase();
        return n.contains("multiplayer") || n.contains("server") || n.contains("joinmulti")
                || n.contains("disconnected") || n.contains("realms");
    }

    public static void drawAll(GuiGraphicsExtractor g, Screen s) {
        if (g == null || s == null) return;
        if (!shouldBrand(s)) return;

        if (isHeavy(s)) {
            drawConnectOverlay(g, s);
        } else if (isInventory(s)) {
            drawInventoryChrome(g, s);
        } else if (isMultiplayer(s)) {
            drawMultiplayerChrome(g, s);
        } else {
            drawTopBar(g, s);
            drawWatermark(g, s);
            drawCornerTicks(g, s);
        }
    }

    public static void drawTopBar(GuiGraphicsExtractor g, Screen s) {
        int ac = accent();
        g.fill(0, 0, s.width, 2, ac);
        g.fill(0, 2, s.width, 4, (ac & 0x00FFFFFF) | 0x55000000);
        g.fill(0, s.height - 2, s.width, s.height, ac);
    }

    public static void drawWatermark(GuiGraphicsExtractor g, Screen s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return;
        Font font = mc.font;
        int ac = accent();
        String mark = "TT Client  v" + TTClient.VERSION;
        int tw = font.width(mark);
        int x = 8;
        int y = Math.max(8, s.height - 18);
        g.fill(x - 3, y - 4, x + tw + 10, y + 12, 0xD0080A0E);
        g.fill(x - 3, y - 4, x - 1, y + 12, ac);
        g.text(font, mark, x + 2, y, ac, false);
    }

    public static void drawCornerTicks(GuiGraphicsExtractor g, Screen s) {
        int ac = accent();
        int t = 14;
        int th = 2;
        // top-left
        g.fill(0, 0, t, th, ac);
        g.fill(0, 0, th, t, ac);
        // top-right
        g.fill(s.width - t, 0, s.width, th, ac);
        g.fill(s.width - th, 0, s.width, t, ac);
        // bottom-left
        g.fill(0, s.height - th, t, s.height, ac);
        g.fill(0, s.height - t, th, s.height, ac);
        // bottom-right
        g.fill(s.width - t, s.height - th, s.width, s.height, ac);
        g.fill(s.width - th, s.height - t, s.width, s.height, ac);
    }

    /** Inventory / container — accent frame + side watermark. */
    public static void drawInventoryChrome(GuiGraphicsExtractor g, Screen s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return;
        Font font = mc.font;
        int ac = accent();
        int dim = (ac & 0x00FFFFFF) | 0x33000000;

        // Full edge rails
        g.fill(0, 0, s.width, 3, ac);
        g.fill(0, s.height - 3, s.width, s.height, ac);
        g.fill(0, 0, 3, s.height, ac);
        g.fill(s.width - 3, 0, s.width, s.height, ac);

        // Soft inner glow under top rail
        g.fill(3, 3, s.width - 3, 6, dim);

        // Top-left brand chip
        String title = "TT CLIENT";
        int tw = font.width(title);
        g.fill(10, 10, 10 + tw + 16, 24, 0xE0080A0E);
        g.fill(10, 10, 12, 24, ac);
        g.text(font, title, 16, 13, ac, false);

        // Bottom-right version
        String ver = "v" + TTClient.VERSION;
        int vw = font.width(ver);
        g.fill(s.width - vw - 18, s.height - 22, s.width - 8, s.height - 8, 0xC0080A0E);
        g.text(font, ver, s.width - vw - 12, s.height - 18, ac, false);

        drawCornerTicks(g, s);
    }

    /** Connecting / loading — big centered TT overlay. */
    public static void drawConnectOverlay(GuiGraphicsExtractor g, Screen s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return;
        Font font = mc.font;
        int ac = accent();

        // Full top + bottom bars
        g.fill(0, 0, s.width, 4, ac);
        g.fill(0, 4, s.width, 28, 0xE002040A);
        g.fill(0, s.height - 28, s.width, s.height - 4, 0xE002040A);
        g.fill(0, s.height - 4, s.width, s.height, ac);

        // Side rails
        g.fill(0, 0, 4, s.height, ac);
        g.fill(s.width - 4, 0, s.width, s.height, ac);

        // Top bar text
        String top = "TT CLIENT  ·  v" + TTClient.VERSION;
        g.text(font, top, 12, 10, ac, false);

        // Center block
        String big = "TT CLIENT";
        String sub = "connecting";
        String n = s.getClass().getName().toLowerCase();
        if (n.contains("loading") || n.contains("progress") || n.contains("download"))
            sub = "loading world";
        if (n.contains("receiving"))
            sub = "receiving terrain";

        int bw = font.width(big);
        int sw = font.width(sub);
        int cx = s.width / 2;
        int cy = s.height / 2;

        // Panel behind text
        int panelW = Math.max(bw, sw) + 48;
        int panelH = 48;
        int px = cx - panelW / 2;
        int py = cy - panelH / 2;
        g.fill(px, py, px + panelW, py + panelH, 0xE0080A0E);
        g.fill(px, py, px + panelW, py + 2, ac);
        g.fill(px, py + panelH - 2, px + panelW, py + panelH, ac);
        g.fill(px, py, px + 2, py + panelH, ac);
        g.fill(px + panelW - 2, py, px + panelW, py + panelH, ac);

        g.text(font, big, cx - bw / 2, cy - 12, ac, false);
        g.text(font, sub, cx - sw / 2, cy + 4, 0xFFB0B8C0, false);

        // Animated pulse bar under panel
        long t = System.currentTimeMillis();
        int pulse = (int) ((t / 8) % Math.max(1, panelW - 8));
        g.fill(px + 4, py + panelH + 6, px + 4 + pulse, py + panelH + 9, ac);

        drawCornerTicks(g, s);
    }

    /** Multiplayer / server list — watermark + top bar + corners. */
    public static void drawMultiplayerChrome(GuiGraphicsExtractor g, Screen s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return;
        Font font = mc.font;
        int ac = accent();

        drawTopBar(g, s);
        drawCornerTicks(g, s);

        String mark = "TT Client";
        int tw = font.width(mark);
        g.fill(s.width - tw - 20, 8, s.width - 8, 22, 0xD0080A0E);
        g.fill(s.width - tw - 20, 8, s.width - tw - 18, 22, ac);
        g.text(font, mark, s.width - tw - 14, 11, ac, false);

        String ver = "v" + TTClient.VERSION;
        g.text(font, ver, 10, s.height - 14, (ac & 0x00FFFFFF) | 0xAA000000, false);
    }
}
