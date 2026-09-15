package com.ttclient.modules.client;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ColorSetting;

public class HUD extends Module {
    public final BoolSetting watermark = addSetting(new BoolSetting("Watermark", "Show TT Client watermark", true));
    public final BoolSetting coords = addSetting(new BoolSetting("Coordinates", "Show coords", true));
    public final BoolSetting fps = addSetting(new BoolSetting("FPS", "Show FPS", true));
    public final BoolSetting direction = addSetting(new BoolSetting("Direction", "Show facing", true));
    public final BoolSetting armor = addSetting(new BoolSetting("Armor", "Show armor status", true));
    public final BoolSetting arrayList = addSetting(new BoolSetting("ArrayList", "Show enabled modules", true));
    public final ColorSetting color = addSetting(new ColorSetting("Color", "HUD color", 0xFF00FFAA));

    public HUD() {
        super("HUD", "Customizable heads-up display", Category.CLIENT);
        setEnabled(true);
    }
}
