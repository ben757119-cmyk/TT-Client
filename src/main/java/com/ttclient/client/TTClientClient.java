package com.ttclient.client;

import com.ttclient.TTClient;
import com.ttclient.gui.ClickGUIScreen;
import com.ttclient.modules.Module;
import com.ttclient.modules.client.ClickGUIModule;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = TTClient.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = TTClient.MOD_ID, value = Dist.CLIENT)
public class TTClientClient {

    public TTClientClient() {
        NeoForge.EVENT_BUS.register(this);
        TTClient.LOGGER.info("TT Client client-side ready");
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        TTClient.LOGGER.info("TT Client setup complete");
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        if (TTClient.modules == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        TTClient.modules.onTick();
    }

    @SubscribeEvent
    public void onKey(InputEvent.Key event) {
        if (TTClient.modules == null) return;
        Minecraft mc = Minecraft.getInstance();
        var screen = mc.gui.screen();
        if (screen != null && !(screen instanceof ClickGUIScreen)) return;

        if (event.getAction() == GLFW.GLFW_PRESS) {
            ClickGUIModule gui = TTClient.modules.getModule(ClickGUIModule.class);
            if (gui != null && event.getKey() == gui.getKeyBind()) {
                if (screen instanceof ClickGUIScreen) {
                    mc.gui.setScreen(null);
                } else if (screen == null) {
                    mc.gui.setScreen(new ClickGUIScreen());
                }
                return;
            }
            for (Module mod : TTClient.modules.getModules()) {
                if (mod.getKeyBind() == event.getKey() && !(mod instanceof ClickGUIModule)) {
                    mod.toggle();
                }
            }
        }
    }
}
