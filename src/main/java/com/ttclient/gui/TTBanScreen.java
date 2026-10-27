package com.ttclient.gui;

import com.ttclient.TTClient;
import com.ttclient.modules.client.ClickGUIModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Custom screen shown when the disconnect reason looks like a ban. */
public class TTBanScreen extends Screen {
    private final String reason;
    private final List<String> reasonLines = new ArrayList<>();
    private int backX, backY, backW, backH;
    private int quitX, quitY, quitW, quitH;

    public TTBanScreen(String reason) {
        super(Component.literal("Banned"));
        this.reason = reason != null && !reason.isEmpty() ? reason : "You are banned from this server.";
    }

    private int accent() { return ClickGUIModule.globalAccent(); }

    @Override
    protected void init() {
        backW = 150;
        backH = 26;
        quitW = 150;
        quitH = 26;
        int gap = 10;
        int total = backW + gap + quitW;
        backX = width / 2 - total / 2;
        quitX = backX + backW + gap;
        backY = quitY = height / 2 + 56;

        reasonLines.clear();
        wrap(reason, Math.min(width - 80, 360));
    }

    private void wrap(String text, int maxW) {
        if (font == null || text == null) {
            reasonLines.add(text != null ? text : "");
            return;
        }
        String[] words = text.replace('\n', ' ').split(" ");
        StringBuilder line = new StringBuilder();
        for (String w : words) {
            String trial = line.isEmpty() ? w : line + " " + w;
            if (font.width(trial) > maxW && !line.isEmpty()) {
                reasonLines.add(line.toString());
                line = new StringBuilder(w);
            } else {
                line = new StringBuilder(trial);
            }
        }
        if (!line.isEmpty()) reasonLines.add(line.toString());
        if (reasonLines.isEmpty()) reasonLines.add(text);
        while (reasonLines.size() > 6) reasonLines.remove(reasonLines.size() - 1);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xFF0A0406);
        // subtle red vignette bars
        g.fill(0, 0, width, 40, 0x44FF2040);
        g.fill(0, height - 40, width, height, 0x44FF2040);
    }

    @Override protected void extractBlurredBackground(GuiGraphicsExtractor g) {}
    @Override public void extractTransparentBackground(GuiGraphicsExtractor g) {}
    @Override protected void extractMenuBackground(GuiGraphicsExtractor g) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        int ac = accent();
        int red = 0xFFFF3355;

        g.fill(0, 0, width, 3, red);
        g.fill(0, height - 3, width, height, red);

        String title = "YOU ARE BANNED";
        g.text(font, title, width / 2 - font.width(title) / 2, height / 2 - 70, red, false);

        String sub = "TT Client  v" + TTClient.VERSION;
        g.text(font, sub, width / 2 - font.width(sub) / 2, height / 2 - 54, 0xFF6B7280, false);

        int boxW = Math.min(width - 48, 400);
        int lineH = 12;
        int boxH = 20 + reasonLines.size() * lineH;
        int boxX = width / 2 - boxW / 2;
        int boxY = height / 2 - 28;

        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xE010080C);
        g.fill(boxX, boxY, boxX + 3, boxY + boxH, red);
        g.fill(boxX, boxY, boxX + boxW, boxY + 1, 0x33FF3355);

        int ty = boxY + 10;
        for (String line : reasonLines) {
            g.text(font, line, width / 2 - font.width(line) / 2, ty, 0xFFE8EAED, false);
            ty += lineH;
        }

        String tip = "This is a server-side ban — TT Client cannot remove it.";
        g.text(font, tip, width / 2 - font.width(tip) / 2, boxY + boxH + 12, 0xFF8B909A, false);

        drawBtn(g, backX, backY, backW, backH, "Back to Title", mx, my, ac);
        drawBtn(g, quitX, quitY, quitW, quitH, "Quit Game", mx, my, red);
    }

    private void drawBtn(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, int mx, int my, int edge) {
        boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + h;
        g.fill(x, y, x + w, y + h, hov ? 0xF01A1020 : 0xE0120A10);
        g.fill(x, y, x + 2, y + h, edge);
        g.text(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, hov ? edge : 0xFFE8EAED, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mx = event.x(), my = event.y();
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return true;

        if (mx >= backX && mx <= backX + backW && my >= backY && my <= backY + backH) {
            try { mc.gui.setScreen(new TTTitleScreen()); }
            catch (Throwable t) {
                try { Minecraft.class.getMethod("setScreen", Screen.class).invoke(mc, new TTTitleScreen()); }
                catch (Throwable ignored) {}
            }
            return true;
        }
        if (mx >= quitX && mx <= quitX + quitW && my >= quitY && my <= quitY + quitH) {
            try { mc.stop(); } catch (Throwable ignored) {}
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** Heuristic: disconnect message is a ban (not a normal kick / timeout). */
    public static boolean looksLikeBan(String reason) {
        if (reason == null || reason.isBlank()) return false;
        String s = reason.toLowerCase();
        if (s.contains("banned") || s.contains("you are ban") || s.contains("ban reason"))
            return true;
        if (s.contains("permanently banned") || s.contains("temporarily banned"))
            return true;
        if (s.contains("blacklist") || s.contains("blacklisted"))
            return true;
        // "ban" alone is noisy (e.g. "bandwidth") — require word-ish match
        if (s.matches(".*\\bban(ned|s)?\\b.*"))
            return true;
        if (s.contains("suspended from") || s.contains("account suspended"))
            return true;
        return false;
    }
}
