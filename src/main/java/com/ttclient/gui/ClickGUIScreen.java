package com.ttclient.gui;

import com.ttclient.TTClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.modules.client.ClickGUIModule;
import com.ttclient.settings.*;
import net.minecraft.client.gui.GuiGraphics;
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
    private String searchQuery = "";

    public ClickGUIScreen() {
        super(Component.literal("TT Client"));
        int x = 20;
        for (Category cat : Category.values()) {
            Panel p = new Panel(cat, x, 20);
            panels.put(cat, p);
            panelList.add(p);
            x += 120;
        }
    }

    /**
     * Vanilla Screen blur is what made Right Shift look smeared.
     * Override so 26.2 never runs applyBlur for this GUI.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x88000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        for (Panel panel : panelList) {
            if (panel.mouseClicked(mouseX, mouseY, button)) return true;
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
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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
        private final int width = 110;
        private final int headerHeight = 16;
        private Module expanded = null;

        public Panel(Category category, double x, double y) {
            this.category = category;
            this.x = x;
            this.y = y;
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
            if (!open || TTClient.modules == null) return false;

            List<Module> mods = TTClient.modules.getModulesByCategory(category);
            int my = (int) y + headerHeight;
            for (Module mod : mods) {
                int h = 14;
                if (mouseX >= x && mouseX <= x + width && mouseY >= my && mouseY <= my + h) {
                    if (button == 0) {
                        mod.toggle();
                        return true;
                    } else if (button == 1) {
                        if (!mod.getSettings().isEmpty()) {
                            expanded = (expanded == mod) ? null : mod;
                        }
                        return true;
                    } else if (button == 2) {
                        startBinding(mod);
                        return true;
                    }
                }
                my += h;
                if (expanded == mod) {
                    for (Setting<?> s : mod.getSettings()) {
                        int sh = 13;
                        if (mouseX >= x + 2 && mouseX <= x + width - 2 && mouseY >= my && mouseY <= my + sh) {
                            if (s instanceof BoolSetting bs) {
                                bs.toggle();
                                return true;
                            } else if (s instanceof ModeSetting ms) {
                                ms.cycle();
                                return true;
                            } else if (s instanceof NumberSetting ns) {
                                double next = ns.get() + ns.getStep();
                                if (next > ns.getMax()) next = ns.getMin();
                                ns.set(next);
                                return true;
                            }
                        }
                        my += sh;
                    }
                }
            }
            return false;
        }

        public void mouseReleased() {
            dragging = false;
        }

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
