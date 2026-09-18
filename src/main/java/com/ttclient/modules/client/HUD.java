package com.ttclient.modules.client;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ColorSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class HUD extends Module {
    public final BoolSetting watermark = addSetting(new BoolSetting("Watermark", "Show TT Client watermark", true));
    public final BoolSetting coords = addSetting(new BoolSetting("Coordinates", "Show coords", true));
    public final BoolSetting fps = addSetting(new BoolSetting("FPS", "Show FPS", true));
    public final BoolSetting direction = addSetting(new BoolSetting("Direction", "Show facing", true));
    public final BoolSetting armor = addSetting(new BoolSetting("Armor", "Show armor status", true));
    public final BoolSetting potion = addSetting(new BoolSetting("Potions", "Show potion effects", true));
    public final BoolSetting arrayList = addSetting(new BoolSetting("ArrayList", "Show enabled modules", true));
    public final BoolSetting nether = addSetting(new BoolSetting("NetherCoords", "Show dimension-converted coords", true));
    public final BoolSetting biome = addSetting(new BoolSetting("Biome", "Show current biome", true));
    public final BoolSetting ping = addSetting(new BoolSetting("Ping", "Show server ping", true));
    public final ColorSetting color = addSetting(new ColorSetting("Color", "HUD color", 0xFF00FFAA));
    public final NumberSetting scale = addSetting(new NumberSetting("Scale", "HUD scale", 1.0, 0.5, 2.0, 0.1));

    public HUD() {
        super("HUD", "Customizable heads-up display", Category.CLIENT);
        setEnabled(true);
    }

    @Override
    public void onRender2D(GuiGraphics g, float partialTick) {
        if (mc.player == null || mc.options.hideGui) return;
        Font font = mc.font;
        int col = color.get();
        int x = 4;
        int y = 4;

        if (watermark.get()) {
            String text = "TT Client §7v" + com.ttclient.TTClient.VERSION;
            g.drawString(font, text, x + 1, y + 1, 0x000000, false);
            g.drawString(font, text, x, y, col, false);
            y += 12;
        }

        if (fps.get()) {
            String text = mc.getFps() + " FPS";
            g.drawString(font, text, x + 1, y + 1, 0x000000, false);
            g.drawString(font, text, x, y, col, false);
            y += 12;
        }

        if (coords.get()) {
            String text = String.format("XYZ §f%.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            g.drawString(font, text, x + 1, y + 1, 0x000000, false);
            g.drawString(font, text, x, y, col, false);
            y += 12;
            BlockPos pos = mc.player.blockPosition();
            String block = String.format("Block §f%d %d %d", pos.getX(), pos.getY(), pos.getZ());
            g.drawString(font, block, x + 1, y + 1, 0x000000, false);
            g.drawString(font, block, x, y, col, false);
            y += 12;
        }

        if (direction.get()) {
            float yaw = Mth.wrapDegrees(mc.player.getYRot());
            String dir = getDir(yaw);
            String text = String.format("Facing §f%s (%.0f)", dir, yaw);
            g.drawString(font, text, x + 1, y + 1, 0x000000, false);
            g.drawString(font, text, x, y, col, false);
            y += 12;
        }

        if (nether.get()) {
            double nx = mc.player.getX();
            double nz = mc.player.getZ();
            String dim = mc.level != null ? mc.level.dimension().location().getPath() : "";
            String text;
            if ("the_nether".equals(dim)) {
                text = String.format("Overworld §f%.1f %.1f", nx * 8.0, nz * 8.0);
            } else {
                text = String.format("Nether §f%.1f %.1f", nx / 8.0, nz / 8.0);
            }
            g.drawString(font, text, x + 1, y + 1, 0x000000, false);
            g.drawString(font, text, x, y, col, false);
            y += 12;
        }

        if (biome.get() && mc.level != null) {
            var holder = mc.level.getBiome(mc.player.blockPosition());
            String biomeName = holder.unwrapKey().map(k -> k.location().getPath().replace('_', ' ')).orElse("unknown");
            String text = "Biome §f" + biomeName;
            g.drawString(font, text, x + 1, y + 1, 0x000000, false);
            g.drawString(font, text, x, y, col, false);
            y += 12;
        }

        if (ping.get() && mc.getConnection() != null && mc.player != null) {
            var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            int ms = info != null ? info.getLatency() : 0;
            String text = ms + " ms";
            g.drawString(font, text, x + 1, y + 1, 0x000000, false);
            g.drawString(font, text, x, y, col, false);
            y += 12;
        }

        if (armor.get()) {
            y += 4;
            for (ItemStack stack : mc.player.getArmorSlots()) {
                if (!stack.isEmpty()) {
                    int max = stack.getMaxDamage();
                    int rem = max - stack.getDamageValue();
                    float pct = max > 0 ? (float) rem / max : 1f;
                    int ac = pct > 0.5f ? 0x55FF55 : (pct > 0.25f ? 0xFFFF55 : 0xFF5555);
                    String text = stack.getHoverName().getString() + " §7" + rem + "/" + max;
                    g.drawString(font, text, x + 1, y + 1, 0x000000, false);
                    g.drawString(font, text, x, y, ac, false);
                    y += 12;
                }
            }
        }

        if (potion.get() && mc.player != null) {
            int py = mc.getWindow().getGuiScaledHeight() / 2 + 40;
            for (var instance : mc.player.getActiveEffects()) {
                String raw = instance.getDescriptionId();
                String nice = raw.contains(".") ? raw.substring(raw.lastIndexOf('.') + 1).replace('_', ' ') : raw;
                int secs = Math.max(0, instance.getDuration() / 20);
                String text = nice + " §7" + (secs / 60) + ":" + String.format("%02d", secs % 60);
                g.drawString(font, text, x, py, col, false);
                py += 11;
            }
        }

        if (arrayList.get()) {
            int ay = 4;
            int screenW = mc.getWindow().getGuiScaledWidth();
            for (Module m : com.ttclient.client.TTClientClient.modules.getModules()) {
                if (m.isEnabled() && !(m instanceof ClickGUIModule) && !(m instanceof HUD)) {
                    String name = m.getName();
                    int w = font.width(name);
                    g.drawString(font, name, screenW - w - 5 + 1, ay + 1, 0x000000, false);
                    g.drawString(font, name, screenW - w - 5, ay, col, false);
                    ay += 11;
                }
            }
        }
    }

    private String getDir(float yaw) {
        if (yaw >= -45 && yaw < 45) return "South";
        if (yaw >= 45 && yaw < 135) return "West";
        if (yaw >= 135 || yaw < -135) return "North";
        return "East";
    }
}
