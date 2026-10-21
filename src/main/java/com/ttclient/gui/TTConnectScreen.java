package com.ttclient.gui;

import com.ttclient.TTClient;
import com.ttclient.modules.client.ClickGUIModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Full-screen connecting / loading — replaces vanilla behind the scenes. */
public class TTConnectScreen extends Screen {
    private final Screen vanilla; // kept for cancel path if needed
    private final String status;
    private long startMs;

    public TTConnectScreen(Screen vanilla, String status) {
        super(Component.literal("Connecting"));
        this.vanilla = vanilla;
        this.status = status != null ? status : "Connecting";
        this.startMs = System.currentTimeMillis();
    }

    private int accent() { return ClickGUIModule.globalAccent(); }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return true; }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xFF05060A);
        // soft particles
        long t = System.currentTimeMillis();
        int ac = accent() & 0x00FFFFFF;
        for (int i = 0; i < 40; i++) {
            int px = (int) ((t / 20 + i * 37) % Math.max(1, width));
            int py = (int) ((t / 30 + i * 53) % Math.max(1, height));
            int a = 0x18 + (i % 5) * 8;
            g.fill(px, py, px + 2, py + 2, (a << 24) | ac);
        }
    }

    @Override protected void extractBlurredBackground(GuiGraphicsExtractor g) {}
    @Override public void extractTransparentBackground(GuiGraphicsExtractor g) {}
    @Override protected void extractMenuBackground(GuiGraphicsExtractor g) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        int ac = accent();
        g.fill(0, 0, width, 3, ac);
        g.fill(0, height - 3, width, height, ac);

        String brand = "TT CLIENT";
        g.text(font, brand, width / 2 - font.width(brand) / 2, height / 2 - 36, ac, false);
        g.text(font, "v" + TTClient.VERSION, width / 2 - font.width("v" + TTClient.VERSION) / 2, height / 2 - 22, 0xFF6B7280, false);

        String st = status;
        g.text(font, st, width / 2 - font.width(st) / 2, height / 2 + 4, 0xFFE8EAED, false);

        // Progress pulse
        int barW = 160;
        int bx = width / 2 - barW / 2;
        int by = height / 2 + 24;
        g.fill(bx, by, bx + barW, by + 4, 0xFF1A1E24);
        long elapsed = System.currentTimeMillis() - startMs;
        int fill = (int) ((elapsed / 12) % barW);
        g.fill(bx, by, bx + fill, by + 4, ac);

        String tip = "esc to cancel";
        g.text(font, tip, width / 2 - font.width(tip) / 2, height - 28, 0xFF4B5563, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        // Prefer going back to multiplayer list
        try {
            Class<?> cls = Class.forName("net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen");
            Object s = null;
            for (var c : cls.getConstructors()) {
                try {
                    if (c.getParameterCount() == 1) s = c.newInstance(new TTTitleScreen());
                } catch (Throwable ignored) {}
            }
            if (s instanceof Screen sc) {
                try { mc.gui.setScreen(sc); return; } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        try { mc.gui.setScreen(new TTTitleScreen()); } catch (Throwable ignored) {}
    }
}
