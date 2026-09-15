package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ColorSetting;

public class StorageESP extends Module {
    public final BoolSetting chests = addSetting(new BoolSetting("Chests", "Highlight chests", true));
    public final BoolSetting enderChests = addSetting(new BoolSetting("Ender Chests", "Highlight ender chests", true));
    public final BoolSetting shulkers = addSetting(new BoolSetting("Shulkers", "Highlight shulkers", true));
    public final BoolSetting barrels = addSetting(new BoolSetting("Barrels", "Highlight barrels", true));
    public final ColorSetting color = addSetting(new ColorSetting("Color", "Storage color", 0xFFFFAA00));

    public StorageESP() {
        super("StorageESP", "Highlight storage blocks", Category.RENDER);
    }
}
