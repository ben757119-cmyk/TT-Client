package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;

public class ViewModel extends Module {
    public final NumberSetting x = addSetting(new NumberSetting("X", "Viewmodel X", 0.0, -2.0, 2.0, 0.05));
    public final NumberSetting y = addSetting(new NumberSetting("Y", "Viewmodel Y", 0.0, -2.0, 2.0, 0.05));
    public final NumberSetting z = addSetting(new NumberSetting("Z", "Viewmodel Z", 0.0, -2.0, 2.0, 0.05));
    public final NumberSetting scale = addSetting(new NumberSetting("Scale", "Viewmodel scale", 1.0, 0.1, 2.0, 0.05));

    public ViewModel() {
        super("ViewModel", "Customize held item position", Category.RENDER);
    }
}
