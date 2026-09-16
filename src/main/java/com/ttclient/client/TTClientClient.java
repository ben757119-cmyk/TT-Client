package com.ttclient.client;

import com.ttclient.TTClient;
import com.ttclient.config.ConfigManager;
import com.ttclient.gui.ClickGUIScreen;
import com.ttclient.modules.Module;
import com.ttclient.modules.ModuleManager;
import com.ttclient.modules.client.ClickGUIModule;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = TTClient.MOD_ID, dist = Dist.CLIENT)
public class TTClientClient {

    public static ModuleManager modules;
    public static ConfigManager config;

    public TTClientClient() {
        modules = new ModuleManager();
        config = new ConfigManager();
        modules.init();
        config.load();
        TTClient.modules = modules;
        TTClient.config = config;
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new HudRenderer());
        TTClient.LOGGER.info("TT Client {} ready with {} modules", TTClient.VERSION, modules.getModules().size());
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        if (modules == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        modules.onTick();
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        if (config != null) config.save();
    }

    @SubscribeEvent
    public void onKey(InputEvent.Key event) {
        if (modules == null) return;
        Minecraft mc = Minecraft.getInstance();
        var screen = mc.gui.screen();
        if (screen != null && !(screen instanceof ClickGUIScreen)) return;

        if (event.getAction() == GLFW.GLFW_PRESS) {
            ClickGUIModule gui = modules.getModule(ClickGUIModule.class);
            if (gui != null && event.getKey() == gui.getKeyBind()) {
                if (screen instanceof ClickGUIScreen) {
                    mc.gui.setScreen(null);
                    if (config != null) config.save();
                } else if (screen == null) {
                    mc.gui.setScreen(new ClickGUIScreen());
                }
                return;
            }

            for (Module mod : modules.getModules()) {
                if (mod.getKeyBind() == event.getKey() && !(mod instanceof ClickGUIModule)) {
                    mod.toggle();
                    if (config != null) config.save();
                }
            }
        }
    }
}
