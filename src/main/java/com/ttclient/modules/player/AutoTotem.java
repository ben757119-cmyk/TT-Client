package com.ttclient.modules.player;

import com.ttclient.gui.ClickGUIScreen;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.InvUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;

/**
 * Reliable AutoTotem — multi-tick state machine so the server sees
 * hotbar select BEFORE offhand swap (no ghost totems).
 *
 * Stages:
 *  IDLE → SELECT (pick totem hotbar) → SWAP (F packet) → RESTORE (old slot) → COOLDOWN
 *  Inventory totems use ClickType.SWAP to offhand in one step.
 */
public class AutoTotem extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting(
            "Delay", "Ticks to wait after a successful equip", 3, 0, 20, 1));
    public final BoolSetting onlyEmpty = addSetting(new BoolSetting(
            "OnlyEmpty", "Only swap when offhand is empty (after pop)", false));
    public final NumberSetting health = addSetting(new NumberSetting(
            "Health", "Force equip at or below this HP (0 = always keep totem)", 0, 0, 20, 1));
    public final BoolSetting hotbarFirst = addSetting(new BoolSetting(
            "HotbarFirst", "Prefer hotbar totems (most reliable)", true));
    public final BoolSetting soft = addSetting(new BoolSetting(
            "Soft", "Don't replace crystal/gapple/shield in offhand unless empty or low HP", false));

    private enum Stage { IDLE, SELECT, SWAP, RESTORE, WAIT }

    private Stage stage = Stage.IDLE;
    private int timer;
    private int totemHotbar = -1;
    private int prevHotbar = -1;
    private int invSlot = -1;

    public AutoTotem() {
        super("AutoTotem", "Auto equip totem in offhand (reliable)", Category.PLAYER);
    }

    @Override
    public void onDisable() {
        reset();
    }

    private void reset() {
        stage = Stage.IDLE;
        timer = 0;
        totemHotbar = -1;
        prevHotbar = -1;
        invSlot = -1;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null) return;
        LocalPlayer p = mc.player;

        if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            if (stage != Stage.IDLE) reset();
            return;
        }

        if (shouldPause(mc)) {
            if (stage != Stage.IDLE && stage != Stage.WAIT) reset();
            return;
        }

        if (stage == Stage.IDLE || stage == Stage.WAIT) {
            if (timer > 0) {
                timer--;
                return;
            }
            if (!shouldEquip(p)) return;
            if (!beginEquip(p)) {
                timer = 2;
                return;
            }
        }

        switch (stage) {
            case SELECT -> {
                if (totemHotbar >= 0) {
                    prevHotbar = p.getInventory().getSelectedSlot();
                    InvUtils.selectHotbar(p, totemHotbar);
                    stage = Stage.SWAP;
                } else {
                    if (invSlot >= 0 && InvUtils.clickSwapToOffhand(p, invSlot)) {
                        stage = Stage.WAIT;
                        timer = Math.max(1, delay.getInt());
                    } else {
                        reset();
                        timer = 2;
                    }
                }
            }
            case SWAP -> {
                InvUtils.sendSwapOffhandPacket(p);
                stage = Stage.RESTORE;
            }
            case RESTORE -> {
                if (prevHotbar >= 0 && prevHotbar <= 8) {
                    InvUtils.selectHotbar(p, prevHotbar);
                }
                stage = Stage.WAIT;
                timer = Math.max(1, delay.getInt());
                totemHotbar = -1;
                invSlot = -1;
                prevHotbar = -1;
            }
            default -> {}
        }
    }

    private boolean shouldEquip(LocalPlayer p) {
        if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return false;
        if (onlyEmpty.get() && !p.getOffhandItem().isEmpty()) return false;
        if (health.get() > 0) {
            float hp = p.getHealth() + p.getAbsorptionAmount();
            if (hp > health.get() && !p.getOffhandItem().isEmpty()) return false;
        }
        if (soft.get() && !p.getOffhandItem().isEmpty()) {
            float hp = p.getHealth() + p.getAbsorptionAmount();
            if (hp > 8.0f) {
                var off = p.getOffhandItem();
                if (off.is(Items.END_CRYSTAL) || off.is(Items.GOLDEN_APPLE)
                        || off.is(Items.ENCHANTED_GOLDEN_APPLE)
                        || off.is(Items.SHIELD)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean beginEquip(LocalPlayer p) {
        totemHotbar = -1;
        invSlot = -1;
        if (hotbarFirst.get()) {
            int hb = InvUtils.findHotbar(p, Items.TOTEM_OF_UNDYING);
            if (hb >= 0) {
                totemHotbar = hb;
                stage = Stage.SELECT;
                return true;
            }
        }
        int slot = InvUtils.findItemSlot(p, Items.TOTEM_OF_UNDYING);
        if (slot < 0) return false;
        if (slot < 9) {
            totemHotbar = slot;
            stage = Stage.SELECT;
            return true;
        }
        invSlot = slot;
        stage = Stage.SELECT;
        return true;
    }

    private boolean shouldPause(Minecraft mc) {
        try {
            var s = mc.gui.screen();
            if (s == null) return false;
            if (s instanceof ClickGUIScreen) return false;
            String n = s.getClass().getName().toLowerCase();
            return n.contains("container") || n.contains("chest") || n.contains("furnace")
                    || n.contains("shulker") || n.contains("hopper") || n.contains("brewing")
                    || n.contains("anvil") || n.contains("beacon") || n.contains("barrel");
        } catch (Throwable t) {
            return false;
        }
    }
}
