package com.ttclient.modules.misc;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
public class SoundVolume extends Module {
    public final NumberSetting master = addSetting(new NumberSetting("Master", "Master volume", 1.0, 0, 1, 0.05));
    public SoundVolume() { super("SoundVolume", "Extra volume controls", Category.MISC); }
}
