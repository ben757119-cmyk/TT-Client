package com.ttclient.gui;

import com.ttclient.TTClient;
import com.ttclient.modules.client.ClickGUIModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Full disconnect / kick screen — no vanilla buttons showing through. */
public class TTDisconnectedScreen extends Screen {
    private final String reason;
    private int btnX, btnY, btnW, btnH;

    public TTDisconnectedScreen(String reason) {
        super(Component.literal("Disconnected"));
        this.reason = reason != null && !reason.isEmpty() ? reason : "Disconnected from server";
    }

    private int accent() { return ClickGUIModule.globalAccent(); }

    @Override
    protected void init() {
        btnW = 160;
        btnH = 26;
        btnX = width / 2 - btnW / 2;
        btnY = height / 2 + 40;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xFF05060A);
    }
    @Override protected void extractBlurredBackground(GuiGraphicsExtractor g) {}
    @Override public void extractTransparentBackground(GuiGraphicsExtractor g) {}
    @Override protected void extractMenuBackground(GuiGraphicsExtractor g) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        int ac = accent();
        g.fill(0, 0, width, 3, ac);
        g.fill(0, height - 3, width, height, ac);

        String title = "DISCONNECTED";
        g.text(font, title, width / 2 - font.width(title) / 2, height / 2 - 40, 0xFFFF5566, false);
        g.text(font, "TT Client  v" + TTClient.VERSION, width / 2 - font.width("TT Client  v" + TTClient.VERSION) / 2, height / 2 - 24, 0xFF6B7280, false);

        // Reason box
        String r = reason;
        if (r.length() > 60) r = r.substring(0, 57) + "...";
        int rw = Math.min(width - 40, Math.max(font.width(r) + 24, 200));
        int rx = width / 2 - rw / 2;
        int ry = height / 2 - 6;
        g.fill(rx, ry, rx + rw, ry + 28, 0xE0080A0E);
        g.fill(rx, ry, rx + 2, ry + 28, ac);
        g.text(font, r, width / 2 - font.width(r) / 2, ry + 10, 0xFFE8EAED, false);

        boolean hov = mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH;
        g.fill(btnX, btnY, btnX + btnW, btnY + btnH, hov ? 0xF01A2030 : 0xE0121620);
        if (hov) g.fill(btnX, btnY, btnX + 2, btnY + btnH, ac);
        String lab = "Back to Menu";
        g.text(font, lab, btnX + (btnW - font.width(lab)) / 2, btnY + 9, hov ? ac : 0xFFE8EAED, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mx = event.x(), my = event.y();
        if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
            goMenu();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        goMenu();
    }

    private void goMenu() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        try { mc.gui.setScreen(new TTTitleScreen()); return; } catch (Throwable ignored) {}
        try { Minecraft.class.getMethod("setScreen", Screen.class).invoke(mc, new TTTitleScreen()); } catch (Throwable ignored) {}
    }
}
