package com.ttclient.modules.combat;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
public class TriggerBot extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Click delay", 0, 0, 10, 1));
    public TriggerBot() { super("TriggerBot", "Attack when crosshair on entity", Category.COMBAT); }
}
