package com.ttclient.modules.client;

import com.ttclient.gui.ClickGUIScreen;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ColorSetting;
import com.ttclient.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class ClickGUIModule extends Module {
    public final ModeSetting style = addSetting(new ModeSetting("Style", "GUI style", "TT Dark", "TT Dark", "Neon", "Classic"));
    public final ColorSetting accent = addSetting(new ColorSetting("Accent", "Accent color", 0xFF00FFAA));
    public final BoolSetting blur = addSetting(new BoolSetting("Blur", "Darker overlay (no vanilla menu blur)", false));

    public ClickGUIModule() {
        super("ClickGUI", "Opens the TT Client ClickGUI", Category.CLIENT);
        setKeyBind(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void onEnable() {
        Minecraft minecraft = mc();
        if (minecraft != null && minecraft.gui.screen() == null) {
            minecraft.gui.setScreen(new ClickGUIScreen());
        }
        setEnabled(false);
    }
}
