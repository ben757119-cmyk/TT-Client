package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class MiddleClickPearl extends Module {
    private boolean wasDown;

    public MiddleClickPearl() {
        super("MiddleClickPearl", "Middle-click throws an ender pearl", Category.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null) return;
        long win = mc.getWindow().handle();
        boolean down = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        if (down && !wasDown) {
            LocalPlayer p = mc.player;
            int prev = p.getInventory().getSelectedSlot();
            for (int i = 0; i < 9; i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (s.is(Items.ENDER_PEARL)) {
                    p.getInventory().setSelectedSlot(i);
                    mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
                    p.getInventory().setSelectedSlot(prev);
                    break;
                }
            }
        }
        wasDown = down;
    }
}
