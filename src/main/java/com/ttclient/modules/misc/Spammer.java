package com.ttclient.modules.misc;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
public class Spammer extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Message delay s", 5, 1, 60, 1));
    public Spammer() { super("Spammer", "Chat spammer", Category.MISC); }
}
