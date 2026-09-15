package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;

public class NoSlow extends Module {
    public final BoolSetting items = addSetting(new BoolSetting("Items", "No slow from using items", true));
    public final BoolSetting webs = addSetting(new BoolSetting("Webs", "No slow in webs", true));
    public final BoolSetting soulsand = addSetting(new BoolSetting("Soul Sand", "No slow on soul sand", true));

    public NoSlow() {
        super("NoSlow", "Remove movement slowdowns", Category.MOVEMENT);
    }
}
