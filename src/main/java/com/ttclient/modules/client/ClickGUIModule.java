package com.ttclient.modules.client;

import com.ttclient.gui.ClickGUIScreen;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ColorSetting;
import org.lwjgl.glfw.GLFW;

public class ClickGUIModule extends Module {
    public final ColorSetting accent = addSetting(new ColorSetting("Accent", "Panel accent color", 0xFF00E8A0));
    public final BoolSetting dim = addSetting(new BoolSetting("SolidDim", "Optional solid shade behind panels. Never vanilla blur.", false));

    public ClickGUIModule() {
        super("ClickGUI", "Opens the TT Client ClickGUI (Right Shift)", Category.CLIENT);
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
