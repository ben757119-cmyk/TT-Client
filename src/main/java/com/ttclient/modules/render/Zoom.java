package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class Zoom extends Module {
    public final NumberSetting zoomFactor = addSetting(new NumberSetting("Factor", "Zoom strength", 0.3, 0.05, 0.8, 0.05));
    private double oldFov = -1;
    private boolean zooming = false;

    public Zoom() {
        super("Zoom", "Hold to zoom in", Category.RENDER);
        setKeyBind(GLFW.GLFW_KEY_C);
        setEnabled(true);
    }

    @Override
    public void onTick() {
        boolean keyDown = getKeyBind() != -1 && GLFW.glfwGetKey(mc.getWindow().getWindow(), getKeyBind()) == GLFW.GLFW_PRESS;
        if (keyDown && !zooming) {
            oldFov = mc.options.fov().get();
            zooming = true;
        } else if (!keyDown && zooming) {
            if (oldFov > 0) mc.options.fov().set(oldFov);
            zooming = false;
            oldFov = -1;
        }
        if (zooming && oldFov > 0) {
            mc.options.fov().set(Math.max(1.0, oldFov * zoomFactor.get()));
        }
    }
}
