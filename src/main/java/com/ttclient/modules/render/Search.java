package com.ttclient.modules.render;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
public class Search extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Search range", 64, 16, 128, 8));
    public Search() { super("Search", "Highlight searched blocks", Category.RENDER); }
}
