package com.ttclient.modules.client;

import com.ttclient.gui.ClickGUIScreen;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ColorSetting;
import com.ttclient.settings.ModeSetting;
import org.lwjgl.glfw.GLFW;

public class ClickGUIModule extends Module {
    public final ModeSetting style = addSetting(new ModeSetting("Style", "GUI style", "TT Dark", "TT Dark", "Neon", "Classic"));
    public final ColorSetting accent = addSetting(new ColorSetting("Accent", "Accent color", 0xFF00FFAA));
    public final BoolSetting dim = addSetting(new BoolSetting("Dim", "Optional solid dim (never vanilla blur)", false));

    public ClickGUIModule() {
        super("ClickGUI", "Opens the TT Client ClickGUI", Category.CLIENT);
        setKeyBind(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void onEnable() {
        if (mc.screen == null) {
            mc.setScreen(new ClickGUIScreen());
        }
        setEnabled(false);
    }
}
