package com.ttclient.modules.client;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ModeSetting;

public class CustomMainMenu extends Module {
    public final BoolSetting customBackground = addSetting(new BoolSetting("Custom Background", "Replace title screen background", true));
    public final BoolSetting customButtons = addSetting(new BoolSetting("Custom Buttons", "Styled buttons", true));
    public final BoolSetting logo = addSetting(new BoolSetting("Logo", "Show TT Client logo", true));
    public final ModeSetting theme = addSetting(new ModeSetting("Theme", "Menu theme", "Dark Neon", "Dark Neon", "Midnight", "Cyber"));

    public CustomMainMenu() {
        super("CustomMainMenu", "Customize the Minecraft title screen", Category.CLIENT);
        setEnabled(true);
    }
}
