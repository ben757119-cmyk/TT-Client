package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;

public class InventoryCleaner extends Module {
    public final BoolSetting dropJunk = addSetting(new BoolSetting("Drop Junk", "Drop useless items", true));

    public InventoryCleaner() {
        super("InventoryCleaner", "Clean inventory of junk", Category.PLAYER);
    }
}
