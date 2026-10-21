package com.ttclient.gui;

import com.ttclient.modules.client.ClickGUIModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Simple confirm dialog — used for Quit Game. */
public class TTConfirmScreen extends Screen {
    private final Screen parent;
    private final String title;
    private final String body;
    private final Runnable onConfirm;
    private int yesX, noX, btnY, btnW, btnH;

    public TTConfirmScreen(Screen parent, String title, String body, Runnable onConfirm) {
        super(Component.literal(title));
        this.parent = parent;
        this.title = title;
        this.body = body;
        this.onConfirm = onConfirm;
    }

    private int accent() { return ClickGUIModule.globalAccent(); }

    @Override
    protected void init() {
        btnW = 90;
        btnH = 24;
        btnY = height / 2 + 20;
        yesX = width / 2 - btnW - 8;
        noX = width / 2 + 8;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xF005060A);
    }
    @Override protected void extractBlurredBackground(GuiGraphicsExtractor g) {}
    @Override public void extractTransparentBackground(GuiGraphicsExtractor g) {}
    @Override protected void extractMenuBackground(GuiGraphicsExtractor g) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        int ac = accent();
        int pw = 280, ph = 120;
        int px = width / 2 - pw / 2, py = height / 2 - ph / 2;
        g.fill(px, py, px + pw, py + ph, 0xF0080A0E);
        g.fill(px, py, px + pw, py + 2, ac);
        g.fill(px, py, px + 2, py + ph, ac);

        g.text(font, title, width / 2 - font.width(title) / 2, py + 18, 0xFFE8EAED, false);
        g.text(font, body, width / 2 - font.width(body) / 2, py + 40, 0xFF8B95A8, false);

        boolean hy = mx >= yesX && mx <= yesX + btnW && my >= btnY && my <= btnY + btnH;
        boolean hn = mx >= noX && mx <= noX + btnW && my >= btnY && my <= btnY + btnH;
        g.fill(yesX, btnY, yesX + btnW, btnY + btnH, hy ? 0xF01A2030 : 0xE0121620);
        g.fill(noX, btnY, noX + btnW, btnY + btnH, hn ? 0xF01A2030 : 0xE0121620);
        if (hy) g.fill(yesX, btnY, yesX + 2, btnY + btnH, ac);
        if (hn) g.fill(noX, btnY, noX + 2, btnY + btnH, ac);
        g.text(font, "Quit", yesX + (btnW - font.width("Quit")) / 2, btnY + 8, hy ? ac : 0xFFE8EAED, false);
        g.text(font, "Cancel", noX + (btnW - font.width("Cancel")) / 2, btnY + 8, hn ? ac : 0xFFE8EAED, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mx = event.x(), my = event.y();
        if (mx >= yesX && mx <= yesX + btnW && my >= btnY && my <= btnY + btnH) {
            if (onConfirm != null) onConfirm.run();
            return true;
        }
        if (mx >= noX && mx <= noX + btnW && my >= btnY && my <= btnY + btnH) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                try { mc.gui.setScreen(parent); } catch (Throwable t) {
                    try { Minecraft.class.getMethod("setScreen", Screen.class).invoke(mc, parent); } catch (Throwable ignored) {}
                }
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
