package com.ttclient.modules.client;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
public class ArrayListMod extends Module {
    public final BoolSetting rainbow = addSetting(new BoolSetting("Rainbow", "Rainbow colors", false));
    public ArrayListMod() { super("ArrayList", "Enabled modules list", Category.CLIENT); setEnabled(true); }
}
