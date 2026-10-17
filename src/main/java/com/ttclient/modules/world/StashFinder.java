package com.ttclient.modules.world;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;

/** Prefer Basefind → ChestStash for density scanning. */
public class StashFinder extends Module {
    public StashFinder() {
        super("StashFinder", "Use Basefind → ChestStash instead", Category.WORLD);
    }
}
