package com.ttclient.client;

import com.ttclient.TTClient;
import com.ttclient.modules.Module;
import com.ttclient.modules.client.ArrayListMod;
import com.ttclient.modules.client.HUD;
import com.ttclient.modules.client.Keystrokes;
import com.ttclient.modules.client.Notifications;
import com.ttclient.modules.combat.Destroyer;
import com.ttclient.modules.render.Crosshair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** In-game HUD — montage / CPVP client style. */
public class HudRenderer {

    private static final int ACCENT = 0xFF00E8A0;
    private static final int TEXT = 0xFFE8EAED;
    private static final int DIM = 0xFF6B7280;
    private static final int PANEL = 0xB0080A0E;

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        try {
            if (mc.gui.screen() != null) return;
        } catch (Throwable ignored) {}

        GuiGraphicsExtractor g = event.getGuiGraphics();
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        HUD hud = TTClient.modules != null ? TTClient.modules.getModule(HUD.class) : null;
        if (hud != null && hud.isEnabled()) {
            drawWatermark(g, font, mc, hud);
        }

        ArrayListMod array = TTClient.modules != null ? TTClient.modules.getModule(ArrayListMod.class) : null;
        if (array != null && array.isEnabled()) {
            drawArrayList(g, font, array, sw);
        }

        // Target HUD when Destroyer is on
        Destroyer dest = TTClient.modules != null ? TTClient.modules.getModule(Destroyer.class) : null;
        if (dest != null && dest.isEnabled()) {
            drawTargetHud(g, font, mc, sw, sh);
        }

        Keystrokes keys = TTClient.modules != null ? TTClient.modules.getModule(Keystrokes.class) : null;
        if (keys != null && keys.isEnabled()) {
            drawKeystrokes(g, font, mc, keys, sw, sh);
        }

        Crosshair ch = TTClient.modules != null ? TTClient.modules.getModule(Crosshair.class) : null;
        if (ch != null && ch.isEnabled()) {
            drawCrosshair(g, sw, sh);
        }

        Notifications notifMod = TTClient.modules != null ? TTClient.modules.getModule(Notifications.class) : null;
        if (notifMod != null && notifMod.isEnabled()) {
            NotificationManager.render(g, font, sw, sh);
        }
    }

    private void drawWatermark(GuiGraphicsExtractor g, Font font, Minecraft mc, HUD hud) {
        String left = "tt";
        String mid = "|";
        String ver = TTClient.VERSION;
        String fps = mc.getFps() + " fps";
        int x = 6;
        int y = 6;
        int pad = 5;
        int h = 14;
        int w = font.width(left) + font.width(" " + mid + " ") + font.width(ver) + 8;
        if (hud.fps != null && hud.fps.get()) w += font.width("  " + fps);
        g.fill(x, y, x + w + pad * 2, y + h, PANEL);
        g.fill(x, y, x + 2, y + h, ACCENT);
        int tx = x + pad + 2;
        g.text(font, left, tx, y + 3, ACCENT, false);
        tx += font.width(left) + 4;
        g.text(font, mid, tx, y + 3, DIM, false);
        tx += font.width(mid) + 4;
        g.text(font, ver, tx, y + 3, TEXT, false);
        tx += font.width(ver);
        if (hud.fps != null && hud.fps.get()) {
            g.text(font, "  " + fps, tx, y + 3, DIM, false);
        }
        int cy = y + h + 4;
        if (hud.coords != null && hud.coords.get() && mc.player != null) {
            String coords = String.format("%.0f %.0f %.0f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ());
            g.text(font, coords, x + 2, cy, DIM, false);
        }
    }

    private void drawArrayList(GuiGraphicsExtractor g, Font font, ArrayListMod mod, int sw) {
        if (TTClient.modules == null) return;
        List<Module> enabled = new ArrayList<>();
        for (Module m : TTClient.modules.getModules()) {
            if (!m.isEnabled()) continue;
            if (m instanceof ArrayListMod || m instanceof HUD || m instanceof Notifications) continue;
            if (m instanceof Keystrokes) continue;
            enabled.add(m);
        }
        enabled.sort(Comparator.comparingInt((Module m) -> -font.width(m.getName())));

        int y = 5;
        long time = System.currentTimeMillis();
        for (int i = 0; i < enabled.size(); i++) {
            Module m = enabled.get(i);
            String name = m.getName();
            int w = font.width(name);
            int x = sw - w - 10;
            int color = ACCENT;
            if (mod.rainbow.get()) {
                float hue = ((time / 16f) + i * 16) % 360 / 360f;
                color = 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 0.55f, 1f);
            }
            g.fill(x - 6, y, sw - 1, y + 12, PANEL);
            g.fill(sw - 2, y, sw, y + 12, color);
            g.text(font, name, x, y + 2, color, false);
            y += 13;
        }
    }

    private void drawTargetHud(GuiGraphicsExtractor g, Font font, Minecraft mc, int sw, int sh) {
        Player self = mc.player;
        Player best = null;
        double bestD = 36 * 36;
        for (Player pl : mc.level.players()) {
            if (pl == self || !pl.isAlive()) continue;
            double d = self.distanceToSqr(pl);
            if (d < bestD) {
                bestD = d;
                best = pl;
            }
        }
        if (best == null) return;

        int boxW = 120;
        int boxH = 34;
        int x = sw / 2 - boxW / 2;
        int y = sh / 2 + 28;

        g.fill(x, y, x + boxW, y + boxH, PANEL);
        g.fill(x, y, x + 2, y + boxH, ACCENT);

        String name = best.getName().getString();
        if (name.length() > 14) name = name.substring(0, 12) + "..";
        g.text(font, name, x + 8, y + 5, TEXT, false);

        float hp = best.getHealth() + best.getAbsorptionAmount();
        float max = Math.max(1f, best.getMaxHealth());
        float pct = Math.max(0f, Math.min(1f, hp / max));
        int barX = x + 8;
        int barY = y + 20;
        int barW = boxW - 16;
        g.fill(barX, barY, barX + barW, barY + 6, 0xFF1A1F28);
        int fill = (int) (barW * pct);
        int hpColor = pct > 0.5f ? ACCENT : (pct > 0.25f ? 0xFFFFB020 : 0xFFFF4466);
        if (fill > 0) g.fill(barX, barY, barX + fill, barY + 6, hpColor);

        String hpText = String.format("%.0f", hp);
        g.text(font, hpText, x + boxW - 8 - font.width(hpText), y + 5, DIM, false);
    }

    private void drawCrosshair(GuiGraphicsExtractor g, int sw, int sh) {
        int cx = sw / 2;
        int cy = sh / 2;
        int gap = 3;
        int len = 5;
        g.fill(cx - gap - len, cy, cx - gap, cy + 1, ACCENT);
        g.fill(cx + gap, cy, cx + gap + len, cy + 1, ACCENT);
        g.fill(cx, cy - gap - len, cx + 1, cy - gap, ACCENT);
        g.fill(cx, cy + gap, cx + 1, cy + gap + len, ACCENT);
    }

    private void drawKeystrokes(GuiGraphicsExtractor g, Font font, Minecraft mc, Keystrokes mod, int sw, int sh) {
        if (mc.options == null) return;
        long window = mc.getWindow().handle();
        int size = 20;
        int gap = 3;
        int baseX = 8;
        int baseY = sh - 90;

        drawKey(g, font, baseX + size + gap, baseY, size, "W", mc.options.keyUp.isDown());
        drawKey(g, font, baseX, baseY + size + gap, size, "A", mc.options.keyLeft.isDown());
        drawKey(g, font, baseX + size + gap, baseY + size + gap, size, "S", mc.options.keyDown.isDown());
        drawKey(g, font, baseX + (size + gap) * 2, baseY + size + gap, size, "D", mc.options.keyRight.isDown());
        drawKey(g, font, baseX, baseY + (size + gap) * 2, size * 3 + gap * 2, "SPACE", mc.options.keyJump.isDown());

        if (mod.mouse.get()) {
            boolean lmb = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean rmb = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            int mx = baseX + (size + gap) * 3 + 6;
            drawKey(g, font, mx, baseY + size + gap, size, "L", lmb);
            drawKey(g, font, mx + size + gap, baseY + size + gap, size, "R", rmb);
        }
    }

    private void drawKey(GuiGraphicsExtractor g, Font font, int x, int y, int w, String label, boolean down) {
        int bg = down ? 0xDD00E8A0 : PANEL;
        int fg = down ? 0xFF04120C : TEXT;
        g.fill(x, y, x + w, y + 18, bg);
        if (down) g.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
        int tw = font.width(label);
        g.text(font, label, x + (w - tw) / 2, y + 5, fg, false);
    }
}
