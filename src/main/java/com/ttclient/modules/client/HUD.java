package com.ttclient.modules.client;

import com.ttclient.TTClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class HUD extends Module {
    public final BoolSetting watermark = addSetting(new BoolSetting("Watermark", "Show TT Client watermark", true));
    public final BoolSetting fps = addSetting(new BoolSetting("FPS", "Show FPS", true));
    public final BoolSetting coords = addSetting(new BoolSetting("Coords", "Show coordinates", true));
    public final BoolSetting direction = addSetting(new BoolSetting("Direction", "Show facing", true));
    public final BoolSetting armor = addSetting(new BoolSetting("Armor", "Show armor durability", false));

    public HUD() {
        super("HUD", "Watermark, FPS, coordinates, direction", Category.CLIENT);
        setEnabled(true);
    }

    @Override
    public void onRender2D(GuiGraphics graphics, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        Font font = mc.font;
        int y = 4;
        int accent = 0xFF00E8A0;
        int text = 0xFFE8EAED;
        int dim = 0xFF8B95A8;

        if (watermark.get()) {
            String w = "TT Client " + TTClient.VERSION;
            graphics.fill(2, y - 1, 6 + font.width(w), y + 10, 0xAA0A0B0F);
            graphics.drawString(font, w, 4, y, accent, false);
            y += 12;
        }
        if (fps.get()) {
            graphics.drawString(font, mc.getFps() + " FPS", 4, y, text, false);
            y += 10;
        }
        if (coords.get()) {
            String s = String.format("XYZ %.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            graphics.drawString(font, s, 4, y, text, false);
            y += 10;
        }
        if (direction.get()) {
            graphics.drawString(font, "Facing " + mc.player.getDirection().getSerializedName(), 4, y, dim, false);
            y += 10;
        }
        if (armor.get()) {
            EquipmentSlot[] slots = {
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
            };
            for (EquipmentSlot slot : slots) {
                ItemStack stack = mc.player.getItemBySlot(slot);
                if (stack.isEmpty() || !stack.isDamageableItem()) continue;
                int max = stack.getMaxDamage();
                int left = max - stack.getDamageValue();
                graphics.drawString(font, slot.getName() + ": " + left, 4, y, text, false);
                y += 10;
            }
        }
    }
}
