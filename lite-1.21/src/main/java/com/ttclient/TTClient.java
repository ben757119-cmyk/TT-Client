package com.ttclient;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(TTClient.MOD_ID)
public class TTClient {
    public static final String MOD_ID = "ttclient";
    public static final String NAME = "TT Client";
    public static final String VERSION = "2.0.0-lite";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TTClient(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("{} v{} NeoForge Lite (1.21.x line)", NAME, VERSION);
    }
}
