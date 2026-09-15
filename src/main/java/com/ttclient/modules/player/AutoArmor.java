package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;

public class AutoArmor extends Module {
    public final BoolSetting preferElytra = addSetting(new BoolSetting("Prefer Elytra", "Prefer elytra over chestplate", false));
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Equip delay (ticks)", 2, 0, 10, 1));

    public AutoArmor() {
        super("AutoArmor", "Automatically equip best armor", Category.PLAYER);
    }
}
