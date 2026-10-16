package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.InvUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;

/** Keeps a totem in offhand via inventory click packets (no client ghosting). */
public class AutoTotem extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting(
            "Delay", "Ticks after a swap before next attempt", 4, 1, 20, 1));

    private int cooldown;

    public AutoTotem() {
        super("AutoTotem", "Real offhand totem — no ghosting", Category.PLAYER);
    }

    @Override
    public void onDisable() {
        cooldown = 0;
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

        if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
        if (InvUtils.hasScreen(mc)) return;

        int slot = InvUtils.findItemSlot(p, Items.TOTEM_OF_UNDYING);
        if (slot < 0) return;

        if (InvUtils.swapToOffhand(p, slot)) {
            cooldown = delay.getInt();
        } else {
            cooldown = delay.getInt();
        }
    }
}
