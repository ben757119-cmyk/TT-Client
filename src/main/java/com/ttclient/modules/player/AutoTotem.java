package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.InvUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;

/**
 * Keeps a totem in offhand using real inventory click packets.
 * No client-only item moves — that was causing ghost totems.
 */
public class AutoTotem extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting(
            "Delay", "Ticks after a swap before next attempt", 4, 1, 20, 1));

    private int cooldown;
    private boolean waitingConfirm;

    public AutoTotem() {
        super("AutoTotem", "Real offhand totem — no ghosting", Category.PLAYER);
    }

    @Override
    public void onDisable() {
        cooldown = 0;
        waitingConfirm = false;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null) return;
        LocalPlayer p = mc.player;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            waitingConfirm = false;
            return;
        }

        // Don't fight open screens (chests etc.)
        if (mc.screen != null) return;

        int slot = InvUtils.findItemSlot(p, Items.TOTEM_OF_UNDYING);
        if (slot < 0) return;

        if (InvUtils.swapToOffhand(p, slot)) {
            waitingConfirm = true;
            cooldown = delay.getInt(); // wait for server before retry — stops ghosts
        } else {
            cooldown = delay.getInt();
        }
    }
}
