package com.ttclient;

import com.mojang.logging.LogUtils;
import com.ttclient.config.ConfigManager;
import com.ttclient.modules.ModuleManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(TTClient.MOD_ID)
public class TTClient {
    public static final String MOD_ID = "ttclient";
    public static final String NAME = "TT Client";
    public static final String VERSION = "1.0.1";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ModuleManager modules;
    public static ConfigManager config;

    public TTClient(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("{} v{} initializing...", NAME, VERSION);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        modules = new ModuleManager();
        config = new ConfigManager();
        modules.init();
        LOGGER.info("{} loaded successfully with {} modules", NAME, modules.getModules().size());
    }
}
