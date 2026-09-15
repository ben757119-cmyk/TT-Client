package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ColorSetting;
import com.ttclient.settings.NumberSetting;

public class HoleESP extends Module {
    public final BoolSetting bedrock = addSetting(new BoolSetting("Bedrock", "Show bedrock holes", true));
    public final BoolSetting obsidian = addSetting(new BoolSetting("Obsidian", "Show obsidian holes", true));
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Scan range", 16, 4, 32, 1));
    public final ColorSetting bedrockColor = addSetting(new ColorSetting("Bedrock Color", "Bedrock hole color", 0xFF00FF00));
    public final ColorSetting obbyColor = addSetting(new ColorSetting("Obsidian Color", "Obsidian hole color", 0xFFFFFF00));

    public HoleESP() {
        super("HoleESP", "Highlight safe holes", Category.RENDER);
    }
}
