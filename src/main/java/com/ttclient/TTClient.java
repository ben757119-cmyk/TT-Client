package com.ttclient;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Common entry point. All gameplay/module state is client-only so a dedicated
 * server can load this jar without touching Minecraft.getInstance().
 */
@Mod(TTClient.MOD_ID)
public class TTClient {
    public static final String MOD_ID = "ttclient";
    public static final String NAME = "TT Client";
    public static final String VERSION = "1.2.0";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TTClient(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("{} v{} common load (client features initialize on Dist.CLIENT)", NAME, VERSION);
    }
}
