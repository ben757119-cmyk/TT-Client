package com.ttclient.modules.render;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.ColorSetting;
public class Crosshair extends Module {
    public final ColorSetting color = addSetting(new ColorSetting("Color", "Crosshair color", 0xFFFFFFFF));
    public Crosshair() { super("Crosshair", "Custom crosshair", Category.RENDER); }
}
