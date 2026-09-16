package com.ttclient.modules.render;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.ColorSetting;
public class ChinaHat extends Module {
    public final ColorSetting color = addSetting(new ColorSetting("Color", "Hat color", 0xAA00FFAA));
    public ChinaHat() { super("ChinaHat", "Render hat above player", Category.RENDER); }
}
