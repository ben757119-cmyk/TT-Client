package com.ttclient.modules.player;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
public class Freecam extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Camera speed", 1.0, 0.1, 5, 0.1));
    public Freecam() { super("Freecam", "Detach camera", Category.PLAYER); }
}
