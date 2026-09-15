package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;

public class NameTags extends Module {
    public final BoolSetting health = addSetting(new BoolSetting("Health", "Show health", true));
    public final BoolSetting armor = addSetting(new BoolSetting("Armor", "Show armor items", true));
    public final BoolSetting ping = addSetting(new BoolSetting("Ping", "Show ping", true));
    public final NumberSetting scale = addSetting(new NumberSetting("Scale", "Nametag scale", 1.0, 0.5, 2.0, 0.1));

    public NameTags() {
        super("NameTags", "Enhanced player nametags", Category.RENDER);
    }
}
