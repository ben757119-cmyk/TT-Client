package com.ttclient.modules.render;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
public class Brightness extends Module {
    public final NumberSetting value = addSetting(new NumberSetting("Value", "Brightness", 1.5, 0.5, 3, 0.1));
    public Brightness() { super("Brightness", "Extra brightness control", Category.RENDER); }
}
