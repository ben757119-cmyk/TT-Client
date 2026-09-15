package com.ttclient.modules.client;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;

public class Notifications extends Module {
    public final BoolSetting moduleToggle = addSetting(new BoolSetting("Module Toggle", "Notify on module toggle", true));
    public final BoolSetting lowArmor = addSetting(new BoolSetting("Low Armor", "Warn when armor is low", true));
    public final NumberSetting duration = addSetting(new NumberSetting("Duration", "Notification duration (s)", 2.5, 0.5, 10.0, 0.5));

    public Notifications() {
        super("Notifications", "In-game notification system", Category.CLIENT);
        setEnabled(true);
    }
}
