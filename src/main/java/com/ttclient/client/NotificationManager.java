package com.ttclient.client;

import com.ttclient.modules.client.StreamerMode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class NotificationManager {
    private static final List<Note> NOTES = new ArrayList<>();

    private NotificationManager() {}

    public static void push(String title, String body) {
        if (StreamerMode.active()) return;
        NOTES.add(0, new Note(title, body, System.currentTimeMillis()));
        while (NOTES.size() > 8) NOTES.remove(NOTES.size() - 1);
    }

    public static void pushToggle(String module, boolean enabled) {
        if (StreamerMode.active()) return;
        // Don't announce StreamerMode itself turning on (would flash on stream)
        if ("StreamerMode".equalsIgnoreCase(module)) return;
        push(module, enabled ? "enabled" : "disabled");
    }

    public static void render(GuiGraphicsExtractor g, Font font, int sw, int sh) {
        if (StreamerMode.active()) {
            NOTES.clear();
            return;
        }
        long now = System.currentTimeMillis();
        int y = sh / 2 - 40;
        Iterator<Note> it = NOTES.iterator();
        while (it.hasNext()) {
            Note n = it.next();
            long age = now - n.time;
            if (age > 2500) {
                it.remove();
                continue;
            }
            float alpha = age < 200 ? age / 200f : (age > 2200 ? (2500 - age) / 300f : 1f);
            int a = Math.max(0, Math.min(255, (int) (alpha * 220)));
            int panel = (a << 24) | 0x0A0B0F;
            String line = n.title + " " + n.body;
            int w = font.width(line) + 16;
            int x = sw - w - 8;
            g.fill(x, y, x + w, y + 18, panel);
            g.fill(x, y, x + 2, y + 18, 0xFF00E8A0);
            g.text(font, line, x + 8, y + 5, 0xFFE8EAED, false);
            y += 22;
        }
    }

    private record Note(String title, String body, long time) {}
}
