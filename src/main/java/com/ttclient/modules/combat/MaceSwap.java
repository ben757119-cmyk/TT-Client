package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.InvUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Swap to mace when falling; restore previous slot on land. */
public class MaceSwap extends Module {
    public final NumberSetting minFall = addSetting(new NumberSetting("MinFall", "Fall distance to swap", 1.0, 0, 15, 0.5));
    public final BoolSetting restore = addSetting(new BoolSetting("Restore", "Restore slot on land", true));

    private int savedSlot = -1;
    private boolean swapped;

    public MaceSwap() {
        super("MaceSwap", "Hotbar swap to mace while falling", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        restoreSlot();
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        LocalPlayer p = mc.player;

        int mace = InvUtils.findHotbarMace(p);
        if (mace < 0) {
            if (swapped && restore.get()) restoreSlot();
            return;
        }

        boolean should = !p.onGround() && p.fallDistance >= minFall.get();
        if (should) {
            if (!swapped) {
                savedSlot = p.getInventory().getSelectedSlot();
                swapped = true;
            }
            if (p.getInventory().getSelectedSlot() != mace) {
                InvUtils.selectHotbar(p, mace);
            }
        } else if (swapped && p.onGround() && restore.get()) {
            restoreSlot();
        }
    }

    private void restoreSlot() {
        Minecraft mc = mc();
        if (mc != null && mc.player != null && savedSlot >= 0) {
            InvUtils.selectHotbar(mc.player, savedSlot);
        }
        savedSlot = -1;
        swapped = false;
    }
}
