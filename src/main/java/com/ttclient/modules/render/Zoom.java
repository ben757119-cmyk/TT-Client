package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class Zoom extends Module {
    public final NumberSetting zoomFactor = addSetting(new NumberSetting("Factor", "Zoom strength", 0.3, 0.05, 0.8, 0.05));
    private int oldFov = -1;
    private boolean zooming = false;

    public Zoom() {
        super("Zoom", "Hold to zoom in", Category.RENDER);
        setKeyBind(GLFW.GLFW_KEY_C);
        setEnabled(true);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.options == null || mc.getWindow() == null) return;
        long window = mc.getWindow().handle();
        boolean keyDown = getKeyBind() != -1 && GLFW.glfwGetKey(window, getKeyBind()) == GLFW.GLFW_PRESS;
        if (keyDown && !zooming) {
            oldFov = mc.options.fov().get();
            zooming = true;
        } else if (!keyDown && zooming) {
            if (oldFov > 0) mc.options.fov().set(oldFov);
            zooming = false;
            oldFov = -1;
        }
        if (zooming && oldFov > 0) {
            int zoomed = Math.max(1, (int) Math.round(oldFov * zoomFactor.get()));
            mc.options.fov().set(zoomed);
        }
    }
}
