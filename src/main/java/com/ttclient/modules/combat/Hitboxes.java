package com.ttclient.modules.combat;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
public class Hitboxes extends Module {
    public final NumberSetting expand = addSetting(new NumberSetting("Expand", "Hitbox expand", 0.1, 0, 1, 0.05));
    public Hitboxes() { super("Hitboxes", "Expand entity hitboxes", Category.COMBAT); }
}
