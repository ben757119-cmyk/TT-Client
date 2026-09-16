package com.ttclient.modules.render;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.ColorSetting;
public class PopChams extends Module {
    public final ColorSetting color = addSetting(new ColorSetting("Color", "Pop color", 0xAAFF5555));
    public PopChams() { super("PopChams", "Totem pop chams", Category.RENDER); }
}
