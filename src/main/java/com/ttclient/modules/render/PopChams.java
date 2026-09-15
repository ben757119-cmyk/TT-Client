package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.ColorSetting;

public class PopChams extends Module {
    public final ColorSetting color = addSetting(new ColorSetting("Color", "Pop chams color", 0xAAFF5555));

    public PopChams() {
        super("PopChams", "Show totem pop chams", Category.RENDER);
    }
}
