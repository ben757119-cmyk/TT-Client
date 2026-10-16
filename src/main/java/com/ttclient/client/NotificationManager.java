package com.ttclient.client;

import com.ttclient.modules.client.StreamerMode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class NotificationManager {
    private static final List<Note> NOTES = new ArrayList<>();
    private static final int ACCENT = 0xFF00E8A0;

    private NotificationManager() {}

    public static void push(String title, String body) {
        if (StreamerMode.active()) return;
        NOTES.add(0, new Note(title, body, System.currentTimeMillis()));
        while (NOTES.size() > 6) NOTES.remove(NOTES.size() - 1);
    }

    public static void pushToggle(String module, boolean enabled) {
        if (StreamerMode.active()) return;
        if ("StreamerMode".equalsIgnoreCase(module)) return;
        push(module, enabled ? "enabled" : "disabled");
    }

    public static void render(GuiGraphicsExtractor g, Font font, int sw, int sh) {
        if (StreamerMode.active()) {
            NOTES.clear();
            return;
        }
        long now = System.currentTimeMillis();
        int y = 40;
        Iterator<Note> it = NOTES.iterator();
        while (it.hasNext()) {
            Note n = it.next();
            long age = now - n.time;
            if (age > 2200) {
                it.remove();
                continue;
            }
            float alpha = age < 150 ? age / 150f : (age > 1900 ? (2200 - age) / 300f : 1f);
            int a = Math.max(0, Math.min(255, (int) (alpha * 230)));
            int panel = (a << 24) | 0x080A0E;
            boolean on = n.body.toLowerCase().contains("enabled");
            String line = n.title;
            String state = n.body;
            int w = Math.max(font.width(line), font.width(state)) + 20;
            int x = sw - w - 10;
            // slide in from right
            float slide = age < 150 ? (1f - alpha) : 0f;
            x += (int) (slide * 40);

            g.fill(x, y, x + w, y + 22, panel);
            g.fill(x, y, x + 2, y + 22, on ? ACCENT : 0xFFFF4466);
            g.text(font, line, x + 8, y + 3, 0xFFE8EAED, false);
            g.text(font, state, x + 8, y + 12, on ? ACCENT : 0xFFFF8899, false);
            y += 26;
        }
    }

    private record Note(String title, String body, long time) {}
}
