package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;

public class FPSBoost extends Module {
    public final BoolSetting entityCulling = addSetting(new BoolSetting("Entity Culling", "Cull offscreen entities", true));
    public final BoolSetting particleLimit = addSetting(new BoolSetting("Particle Limit", "Limit particles", true));
    public final NumberSetting maxParticles = addSetting(new NumberSetting("Max Particles", "Max particles", 100, 10, 1000, 10));
    public final BoolSetting optimiseChunk = addSetting(new BoolSetting("Chunk Optimise", "Better chunk loading", true));
    public final BoolSetting unfocusedCPU = addSetting(new BoolSetting("Unfocused CPU", "Reduce CPU when unfocused", true));
    public final BoolSetting fastWorldLoad = addSetting(new BoolSetting("Fast World Load", "Faster world loading", true));

    public FPSBoost() {
        super("FPSBoost", "Multiple performance optimisations", Category.MISC);
        setEnabled(true);
    }
}
