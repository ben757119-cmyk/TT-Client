package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.ColorSetting;
import com.ttclient.settings.NumberSetting;

public class BlockHighlight extends Module {
    public final ColorSetting color = addSetting(new ColorSetting("Color", "Highlight color", 0xFF00FFAA));
    public final NumberSetting width = addSetting(new NumberSetting("Width", "Line width", 1.5, 0.5, 5.0, 0.5));

    public BlockHighlight() {
        super("BlockHighlight", "Custom block outline", Category.RENDER);
    }
}
