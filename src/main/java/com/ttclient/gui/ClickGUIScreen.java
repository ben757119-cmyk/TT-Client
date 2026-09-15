package com.ttclient.gui;

import com.ttclient.TTClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.modules.client.ClickGUIModule;
import com.ttclient.settings.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClickGUIScreen extends Screen {
    private final Map<Category, Panel> panels = new HashMap<>();
    private final List<Panel> panelList = new ArrayList<>();
    private Module bindingModule = null;

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

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x88000000);
        g.drawCenteredString(font, "\u00a7bTT Client \u00a77v" + TTClient.VERSION + " \u00a78| \u00a7fRight Shift to close", width / 2, 6, 0xFFFFFF);

        for (Panel panel : panelList) {
            panel.render(g, mouseX, mouseY, font);
        }

        for (Panel panel : panelList) {
            Module hovered = panel.getHoveredModule(mouseX, mouseY);
            if (hovered != null) {
                String desc = hovered.getDescription();
                int tw = font.width(desc);
                g.fill(mouseX + 8, mouseY - 12, mouseX + 12 + tw, mouseY + 2, 0xEE000000);
                g.drawString(font, desc, mouseX + 10, mouseY - 10, 0xFFAAAAAA, false);
                break;
            }
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Panel panel : panelList) {
            if (panel.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Panel panel : panelList) panel.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        for (Panel panel : panelList) {
            if (panel.mouseDragged(mouseX, mouseY, button)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
        return super.keyPressed(keyCode, scanCode, modifiers);
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
        private double dragX, dragY;
        private final int width = 110;
        private final int headerHeight = 16;
        private Module expanded = null;

        public Panel(Category category, double x, double y) {
            this.category = category;
            this.x = x;
            this.y = y;
        }

        public void render(GuiGraphics g, int mouseX, int mouseY, net.minecraft.client.gui.Font font) {
            int accent = 0xFF00FFAA;
            ClickGUIModule guiMod = TTClient.modules.getModule(ClickGUIModule.class);
            if (guiMod != null) accent = guiMod.accent.get();

            g.fill((int) x, (int) y, (int) x + width, (int) y + headerHeight, 0xFF111111);
            g.fill((int) x, (int) y, (int) x + 2, (int) y + headerHeight, accent);
            g.drawString(font, category.name, (int) x + 6, (int) y + 4, accent, false);
            g.drawString(font, open ? "-" : "+", (int) x + width - 12, (int) y + 4, 0xFFAAAAAA, false);

            if (!open) return;

            List<Module> mods = TTClient.modules.getModulesByCategory(category);
            int my = (int) y + headerHeight;

            for (Module mod : mods) {
                int h = 14;
                boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= my && mouseY <= my + h;
                int bg = mod.isEnabled() ? 0xFF1A2A2A : (hovered ? 0xFF222222 : 0xFF181818);
                g.fill((int) x, my, (int) x + width, my + h, bg);
                g.drawString(font, mod.getName(), (int) x + 6, my + 3, mod.isEnabled() ? accent : 0xFFCCCCCC, false);

                if (!mod.getSettings().isEmpty()) {
                    g.drawString(font, expanded == mod ? "\u25be" : "\u25b8", (int) x + width - 12, my + 3, 0xFF888888, false);
                }

                my += h;

                if (expanded == mod) {
                    for (Setting<?> s : mod.getSettings()) {
                        int sh = 13;
                        g.fill((int) x + 2, my, (int) x + width - 2, my + sh, 0xFF101010);
                        String label = s.getName() + ": ";
                        if (s instanceof BoolSetting bs) label += bs.get() ? "\u00a7aON" : "\u00a7cOFF";
                        else if (s instanceof NumberSetting ns) label += String.format("%.2f", ns.get());
                        else if (s instanceof ModeSetting ms) label += ms.get();
                        else if (s instanceof ColorSetting) label += "Color";
                        g.drawString(font, label, (int) x + 8, my + 2, 0xFFAAAAAA, false);
                        my += sh;
                    }
                }
            }

            g.fill((int) x, my, (int) x + width, my + 1, accent);
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight) {
                if (button == 0) {
                    dragging = true;
                    dragX = mouseX - x;
                    dragY = mouseY - y;
                    return true;
                } else if (button == 1) {
                    open = !open;
                    return true;
                }
            }

            if (!open) return false;

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

        public void mouseReleased(double mouseX, double mouseY, int button) {
            dragging = false;
        }

        public boolean mouseDragged(double mouseX, double mouseY, int button) {
            if (dragging) {
                x = mouseX - dragX;
                y = mouseY - dragY;
                return true;
            }
            return false;
        }

        public Module getHoveredModule(int mouseX, int mouseY) {
            if (!open) return null;
            List<Module> mods = TTClient.modules.getModulesByCategory(category);
            int my = (int) y + headerHeight;
            for (Module mod : mods) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= my && mouseY <= my + 14) {
                    return mod;
                }
                my += 14;
                if (expanded == mod) my += mod.getSettings().size() * 13;
            }
            return null;
        }
    }
}
