package com.ttclient.modules.player;

import com.ttclient.client.NotificationManager;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Ominous vault helper.
 *
 * Cannot force vault loot (server-sided). Does:
 * - auto-use Ominous Trial Key when looking at an ominous vault
 * - select / prefer a hotbar slot when a mace is present
 */
public class AutoMace extends Module {
    public final BoolSetting autoKey = addSetting(new BoolSetting(
            "AutoKey", "Use ominous trial key on ominous vaults", true));
    public final BoolSetting autoSelect = addSetting(new BoolSetting(
            "AutoSelect", "Select mace in hotbar when you have one", true));
    public final NumberSetting slot = addSetting(new NumberSetting(
            "Slot", "Preferred hotbar slot (1-9)", 1, 1, 9, 1));
    public final BoolSetting notify = addSetting(new BoolSetting(
            "Notify", "Toast when a mace appears", true));

    private int keyCooldown;
    private boolean hadMace;

    public AutoMace() {
        super("AutoMace",
                "Auto-key ominous vaults and select maces in hotbar",
                Category.PLAYER);
    }

    @Override
    public void onEnable() {
        keyCooldown = 0;
        Minecraft mc = mc();
        hadMace = mc != null && mc.player != null && findHotbarMace(mc.player) >= 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null || mc.level == null) return;
        LocalPlayer player = mc.player;

        if (keyCooldown > 0) keyCooldown--;

        if (autoKey.get() && keyCooldown <= 0) {
            tryUseKeyOnVault(mc, player);
        }

        if (autoSelect.get()) {
            int maceSlot = findHotbarMace(player);
            if (maceSlot >= 0) {
                if (!hadMace && notify.get()) {
                    NotificationManager.push("AutoMace", "Mace ready in hotbar");
                }
                hadMace = true;
                int prefer = slot.getInt() - 1;
                if (maceSlot == prefer || player.getInventory().getSelectedSlot() != maceSlot) {
                    // Prefer configured slot if that slot has mace, else the found slot
                    if (isMace(player.getInventory().getItem(prefer))) {
                        player.getInventory().setSelectedSlot(prefer);
                    } else {
                        player.getInventory().setSelectedSlot(maceSlot);
                    }
                }
            } else {
                hadMace = false;
            }
        }
    }

    private void tryUseKeyOnVault(Minecraft mc, LocalPlayer player) {
        if (!(mc.hitResult instanceof BlockHitResult bhr)) return;
        if (mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockState state = mc.level.getBlockState(bhr.getBlockPos());
        String id = state.getBlock().toString().toLowerCase();
        if (!id.contains("ominous_vault")) return;

        int keySlot = findHotbarNamed(player, "ominous_trial_key");
        if (keySlot < 0) return;

        int prev = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(keySlot);
        try {
            mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, bhr);
        } catch (Throwable t) {
            try {
                mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            } catch (Throwable ignored) {}
        }
        player.getInventory().setSelectedSlot(prev);
        keyCooldown = 15;
    }

    private static int findHotbarMace(LocalPlayer player) {
        return findHotbarNamed(player, "mace");
    }

    private static int findHotbarNamed(LocalPlayer player, String part) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem().toString().toLowerCase().contains(part)) return i;
        }
        return -1;
    }

    private static boolean isMace(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem().toString().toLowerCase().contains("mace");
    }
}
