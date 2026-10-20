package com.ttclient.client;

import com.ttclient.modules.client.StreamerMode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Animated toast notifications (slide + fade). */
public final class NotificationManager {
    private static final List<Note> NOTES = new ArrayList<>();
    private static final int ACCENT = 0xFF00E8A0;
    private static final long LIFE_MS = 2800;
    private static final long IN_MS = 220;
    private static final long OUT_MS = 320;

    private NotificationManager() {}

    public static void push(String title, String body) {
        if (StreamerMode.active()) return;
        NOTES.add(0, new Note(title, body, System.currentTimeMillis(), false));
        while (NOTES.size() > 8) NOTES.remove(NOTES.size() - 1);
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
        int y = 36;
        Iterator<Note> it = NOTES.iterator();
        while (it.hasNext()) {
            Note n = it.next();
            long age = now - n.time;
            if (age > LIFE_MS) {
                it.remove();
                continue;
            }

            float alpha;
            float slide; // 0 = fully on screen, 1 = fully off to the right
            if (age < IN_MS) {
                float t = age / (float) IN_MS;
                // ease out
                t = 1f - (1f - t) * (1f - t);
                alpha = t;
                slide = 1f - t;
            } else if (age > LIFE_MS - OUT_MS) {
                float t = (age - (LIFE_MS - OUT_MS)) / (float) OUT_MS;
                t = t * t;
                alpha = 1f - t;
                slide = t;
            } else {
                alpha = 1f;
                slide = 0f;
            }

            int a = Math.max(0, Math.min(255, (int) (alpha * 235)));
            int panel = (a << 24) | 0x080A0E;
            boolean on = n.body.toLowerCase().contains("enabled") || n.body.toLowerCase().contains("added");
            boolean bad = n.body.toLowerCase().contains("disabled") || n.body.toLowerCase().contains("full")
                    || n.body.toLowerCase().contains("fail") || n.body.toLowerCase().contains("removed");
            int bar = on && !bad ? ACCENT : (bad ? 0xFFFF4466 : 0xFF6B8CFF);

            int textW = Math.max(font.width(n.title), font.width(n.body));
            int w = textW + 22;
            int h = 26;
            int x = sw - w - 12 + (int) (slide * (w + 24));

            // soft shadow
            g.fill(x + 2, y + 2, x + w + 2, y + h + 2, (a / 3) << 24);
            g.fill(x, y, x + w, y + h, panel);
            g.fill(x, y, x + 3, y + h, (a << 24) | (bar & 0x00FFFFFF));
            g.text(font, n.title, x + 10, y + 4, (a << 24) | 0x00E8EAED, false);
            g.text(font, n.body, x + 10, y + 14, (a << 24) | (bar & 0x00FFFFFF), false);
            y += h + 6;
        }
    }

    private record Note(String title, String body, long time, boolean reserved) {}
}
