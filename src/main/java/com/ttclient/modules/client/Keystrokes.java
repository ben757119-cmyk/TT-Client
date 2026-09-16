package com.ttclient.modules.client;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.ColorSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class Keystrokes extends Module {
    public final NumberSetting offsetX = addSetting(new NumberSetting("X", "Horizontal offset", 4, 0, 400, 1));
    public final NumberSetting offsetY = addSetting(new NumberSetting("Y", "Vertical offset from bottom", 50, 0, 400, 1));
    public final ColorSetting color = addSetting(new ColorSetting("Color", "Key color", 0xFF00FFAA));

    public Keystrokes() {
        super("Keystrokes", "On-screen WASD / mouse indicators", Category.CLIENT);
    }

    @Override
    public void onRender2D(GuiGraphics g, float partialTick) {
        if (mc.player == null || mc.options.hideGui) return;
        long win = mc.getWindow().getWindow();
        int x = offsetX.getInt();
        int y = mc.getWindow().getGuiScaledHeight() - offsetY.getInt();
        drawKey(g, x + 16, y, "W", pressed(win, GLFW.GLFW_KEY_W));
        drawKey(g, x, y + 16, "A", pressed(win, GLFW.GLFW_KEY_A));
        drawKey(g, x + 16, y + 16, "S", pressed(win, GLFW.GLFW_KEY_S));
        drawKey(g, x + 32, y + 16, "D", pressed(win, GLFW.GLFW_KEY_D));
        drawKey(g, x + 16, y + 32, "Space", pressed(win, GLFW.GLFW_KEY_SPACE));
    }

    private boolean pressed(long win, int key) {
        return GLFW.glfwGetKey(win, key) == GLFW.GLFW_PRESS;
    }

    private void drawKey(GuiGraphics g, int x, int y, String label, boolean on) {
        int bg = on ? 0xCC00FFAA : 0xAA111111;
        int w = Math.max(14, mc.font.width(label) + 6);
        g.fill(x, y, x + w, y + 14, bg);
        g.drawCenteredString(mc.font, label, x + w / 2, y + 3, on ? 0xFF00110A : color.get());
    }
}
