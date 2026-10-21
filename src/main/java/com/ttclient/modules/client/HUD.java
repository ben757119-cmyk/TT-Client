package com.ttclient.modules.client;

import com.ttclient.TTClient;
import com.ttclient.client.NotificationManager;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Clean info stack — top-left card. */
public class HUD extends Module {
    public final BoolSetting watermark = addSetting(new BoolSetting("Watermark", "Show TT Client watermark", true));
    public final BoolSetting fps = addSetting(new BoolSetting("FPS", "Show FPS", true));
    public final BoolSetting coords = addSetting(new BoolSetting("Coords", "Show coordinates", true));
    public final BoolSetting direction = addSetting(new BoolSetting("Direction", "Show facing", true));
    public final BoolSetting armor = addSetting(new BoolSetting("Armor", "Show armor durability bars", true));
    public final BoolSetting toasts = addSetting(new BoolSetting("Toasts", "Animated notifications", true));

    public HUD() {
        super("HUD", "Clean watermark + info stack", Category.CLIENT);
        setEnabled(true);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.font == null) return;
        Font font = mc.font;
        int ac = ClickGUIModule.globalAccent();
        int text = 0xFFE8EAED;
        int dim = 0xFF8B95A8;

        int x = 6;
        int y = 6;
        int lineH = 11;

        // Watermark chip
        if (watermark.get()) {
            String left = "TT";
            String ver = TTClient.VERSION;
            int w = font.width(left) + font.width(" " + ver) + 14;
            if (fps.get()) w += font.width("  " + mc.getFps() + "fps") + 4;
            g.fill(x, y, x + w, y + 14, 0xD0080A0E);
            g.fill(x, y, x + 2, y + 14, ac);
            int tx = x + 6;
            g.text(font, left, tx, y + 3, ac, false);
            tx += font.width(left) + 4;
            g.text(font, ver, tx, y + 3, dim, false);
            tx += font.width(ver);
            if (fps.get()) {
                g.text(font, "  " + mc.getFps() + "fps", tx, y + 3, text, false);
            }
            y += 18;
        } else if (fps.get()) {
            g.text(font, mc.getFps() + " fps", x + 2, y, text, false);
            y += lineH;
        }

        if (coords.get()) {
            String s = String.format("%.0f  %.0f  %.0f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ());
            g.text(font, s, x + 2, y, text, false);
            y += lineH;
        }
        if (direction.get()) {
            String dir = mc.player.getDirection().getSerializedName();
            g.text(font, dir.toUpperCase(), x + 2, y, dim, false);
            y += lineH;
        }

        if (armor.get()) {
            y += 2;
            EquipmentSlot[] slots = {
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
            };
            for (EquipmentSlot slot : slots) {
                ItemStack stack = mc.player.getItemBySlot(slot);
                if (stack.isEmpty() || !stack.isDamageableItem()) continue;
                int max = stack.getMaxDamage();
                int left = max - stack.getDamageValue();
                float pct = max <= 0 ? 1f : left / (float) max;
                int barW = 40;
                int fill = (int) (barW * pct);
                int col = pct > 0.4f ? ac : (pct > 0.2f ? 0xFFE8C04A : 0xFFE05050);
                g.fill(x + 2, y + 2, x + 2 + barW, y + 6, 0xFF1A1E24);
                if (fill > 0) g.fill(x + 2, y + 2, x + 2 + fill, y + 6, col);
                y += 9;
            }
        }

        if (toasts.get()) {
            try {
                int sw = mc.getWindow().getGuiScaledWidth();
                int sh = mc.getWindow().getGuiScaledHeight();
                NotificationManager.render(g, font, sw, sh);
            } catch (Throwable ignored) {}
        }
    }
}
