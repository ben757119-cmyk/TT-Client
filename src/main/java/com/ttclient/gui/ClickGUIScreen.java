package com.ttclient.gui;

import com.ttclient.TTClient;
import com.ttclient.client.TTClientClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.modules.client.ClickGUIModule;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ModeSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.settings.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClickGUIScreen extends Screen {
    private final Map<Category, Panel> panels = new HashMap<>();
    private final List<Panel> panelList = new ArrayList<>();
    private Module bindingModule = null;
    private Module hoveredModule = null;
    private String searchQuery = "";

    private static final int PANEL_BG = 0xF0121620;
    private static final int HEADER_BG = 0xFF0E1A16;
    private static final int ACCENT = 0xFF00E8A0;
    private static final int TEXT_DIM = 0xFF8B95A8;
    private static final int ROW_ON = 0xFF00E8A0;
    private static final int ROW_OFF = 0xFF9AA3B5;

    public ClickGUIScreen() {
        super(Component.literal("TT Client"));
        int x = 14;
        for (Category cat : Category.values()) {
            Panel p = new Panel(cat, x, 36);
            panels.put(cat, p);
            panelList.add(p);
            x += 122;
        }
    }

    @Override public boolean isInGameUi() { return true; }
    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int overlay = 0x00000000;
        ClickGUIModule guiMod = TTClientClient.modules != null
                ? TTClientClient.modules.getModule(ClickGUIModule.class) : null;
        if (guiMod == null || guiMod.dim.get()) {
            overlay = 0x660A0B0F;
        }
        if (overlay != 0) {
            graphics.fill(0, 0, this.width, this.height, overlay);
        }
    }

    @Override
    protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        hoveredModule = null;
        String searchLabel = searchQuery.isEmpty() ? "type to search" : "search: " + searchQuery;
        graphics.text(this.font,
                "TT Client v" + TTClient.VERSION + "  ·  LMB toggle  ·  RMB settings  ·  MMB bind  ·  " + searchLabel,
                14, 10, TEXT_DIM, false);
        for (Panel panel : panelList) {
            panel.render(graphics, mouseX, mouseY);
        }
        if (hoveredModule != null) {
            String title = hoveredModule.getName();
            String desc = hoveredModule.getDescription();
            int pad = 8;
            int tw = Math.max(font.width(title), font.width(desc));
            int boxW = tw + pad * 2;
            int boxH = 28;
            int tx = Math.min(mouseX + 12, this.width - boxW - 4);
            int ty = Math.min(mouseY + 12, this.height - boxH - 4);
            graphics.fill(tx, ty, tx + boxW, ty + boxH, 0xF0000000);
            graphics.fill(tx, ty, tx + 2, ty + boxH, ACCENT);
            graphics.text(font, title, tx + pad, ty + 4, ACCENT, false);
            graphics.text(font, desc, tx + pad, ty + 15, TEXT_DIM, false);
        }
        if (bindingModule != null) {
            String msg = "Bind: " + bindingModule.getName() + "  (ESC clear)";
            int tw = this.font.width(msg);
            graphics.fill(this.width / 2 - tw / 2 - 10, this.height - 30, this.width / 2 + tw / 2 + 10, this.height - 12, 0xEE000000);
            graphics.text(this.font, msg, this.width / 2 - tw / 2, this.height - 25, ACCENT, false);
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        if (TTClientClient.config != null) TTClientClient.config.save();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        for (int i = panelList.size() - 1; i >= 0; i--) {
            if (panelList.get(i).mouseClicked(mouseX, mouseY, button)) return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (Panel panel : panelList) panel.mouseReleased();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        for (Panel panel : panelList) {
            if (panel.mouseDragged(mouseX, mouseY)) return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (bindingModule != null) {
            if (keyCode == 256) bindingModule.setKeyBind(-1);
            else bindingModule.setKeyBind(keyCode);
            bindingModule = null;
            return true;
        }
        if (keyCode == 256 || keyCode == 344) {
            onClose();
            return true;
        }
        if (keyCode == 259 && !searchQuery.isEmpty()) {
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            return true;
        }
        if (searchQuery.length() < 24) {
            if (keyCode >= 65 && keyCode <= 90) {
                searchQuery += (char) ('a' + (keyCode - 65));
                return true;
            }
            if (keyCode >= 48 && keyCode <= 57) {
                searchQuery += (char) ('0' + (keyCode - 48));
                return true;
            }
            if (keyCode == 32) {
                searchQuery += ' ';
                return true;
            }
        }
        return super.keyPressed(event);
    }

    public void startBinding(Module module) {
        this.bindingModule = module;
    }

    public class Panel {
        private final Category category;
        private double x, y;
        private boolean open = true;
        private boolean dragging = false;
        private double dragOffsetX, dragOffsetY;
        private final int width = 114;
        private final int headerHeight = 18;
        private Module expanded = null;

        public Panel(Category category, double x, double y) {
            this.category = category;
            this.x = x;
            this.y = y;
        }

        private List<Module> filteredModules() {
            List<Module> base = TTClientClient.modules != null
                    ? TTClientClient.modules.getModulesByCategory(category)
                    : List.of();
            if (searchQuery == null || searchQuery.isBlank()) return base;
            String q = searchQuery.toLowerCase();
            List<Module> out = new ArrayList<>();
            for (Module m : base) {
                if (m.getName().toLowerCase().contains(q) || m.getDescription().toLowerCase().contains(q)) {
                    out.add(m);
                }
            }
            return out;
        }

        public void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
            int ix = (int) x;
            int iy = (int) y;
            List<Module> mods = filteredModules();
            int bodyH = headerHeight;
            if (open) {
                for (Module mod : mods) {
                    bodyH += 15;
                    if (expanded == mod) bodyH += mod.getSettings().size() * 13;
                }
            }
            g.fill(ix + 2, iy + 2, ix + width + 2, iy + bodyH + 2, 0x44000000);
            g.fill(ix, iy, ix + width, iy + bodyH, PANEL_BG);
            g.fill(ix, iy, ix + width, iy + headerHeight, HEADER_BG);
            g.fill(ix, iy, ix + 2, iy + bodyH, ACCENT);
            g.text(font, category.name, ix + 8, iy + 5, ACCENT, false);
            if (!open) return;
            int my = iy + headerHeight;
            for (Module mod : mods) {
                boolean on = mod.isEnabled();
                int color = on ? ROW_ON : ROW_OFF;
                boolean hover = mouseX >= ix && mouseX <= ix + width && mouseY >= my && mouseY <= my + 15;
                if (hover) {
                    g.fill(ix + 2, my, ix + width, my + 15, 0x18FFFFFF);
                    hoveredModule = mod;
                }
                g.text(font, mod.getName(), ix + 8, my + 4, color, false);
                if (on) g.fill(ix + width - 11, my + 5, ix + width - 5, my + 11, ACCENT);
                my += 15;
                if (expanded == mod) {
                    for (Setting<?> s : mod.getSettings()) {
                        String label = s.getName() + ": " + String.valueOf(s.get());
                        g.text(font, label, ix + 12, my + 2, TEXT_DIM, false);
                        my += 13;
                    }
                }
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight) {
                if (button == 0) {
                    dragging = true;
                    dragOffsetX = mouseX - x;
                    dragOffsetY = mouseY - y;
                    return true;
                } else if (button == 1) {
                    open = !open;
                    return true;
                }
            }
            if (!open || TTClientClient.modules == null) return false;
            List<Module> mods = filteredModules();
            int my = (int) y + headerHeight;
            for (Module mod : mods) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= my && mouseY <= my + 15) {
                    if (button == 0) { mod.toggle(); return true; }
                    if (button == 1) {
                        if (!mod.getSettings().isEmpty()) expanded = (expanded == mod) ? null : mod;
                        return true;
                    }
                    if (button == 2) { startBinding(mod); return true; }
                }
                my += 15;
                if (expanded == mod) {
                    for (Setting<?> s : mod.getSettings()) {
                        if (mouseX >= x + 2 && mouseX <= x + width - 2 && mouseY >= my && mouseY <= my + 13) {
                            if (s instanceof BoolSetting bs) { bs.toggle(); return true; }
                            if (s instanceof ModeSetting ms) { ms.cycle(); return true; }
                            if (s instanceof NumberSetting ns) {
                                double next = ns.get() + ns.getStep();
                                if (next > ns.getMax()) next = ns.getMin();
                                ns.set(next);
                                return true;
                            }
                        }
                        my += 13;
                    }
                }
            }
            return false;
        }

        public void mouseReleased() { dragging = false; }

        public boolean mouseDragged(double mouseX, double mouseY) {
            if (dragging) {
                x = mouseX - dragOffsetX;
                y = mouseY - dragOffsetY;
                return true;
            }
            return false;
        }
    }
}
