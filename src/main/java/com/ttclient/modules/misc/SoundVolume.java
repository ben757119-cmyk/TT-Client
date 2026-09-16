package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

public class SoundVolume extends Module {
    public final NumberSetting master = addSetting(new NumberSetting("Master", "Master volume %", 50, 0, 100, 5));

    public SoundVolume() {
        super("SoundVolume", "Force master sound volume", Category.MISC);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.options == null) return;
        try {
            mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(master.get() / 100.0);
        } catch (Exception ignored) {}
    }
}
