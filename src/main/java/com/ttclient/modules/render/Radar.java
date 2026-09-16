package com.ttclient.modules.render;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
public class Radar extends Module {
    public final NumberSetting scale = addSetting(new NumberSetting("Scale", "Radar scale", 1.0, 0.5, 2, 0.1));
    public Radar() { super("Radar", "Minimap radar", Category.RENDER); }
}
