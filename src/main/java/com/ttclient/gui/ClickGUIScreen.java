package com.ttclient.gui;

import com.ttclient.TTClient;
import com.ttclient.client.TTClientClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
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

/**
 * In-game ClickGUI for Minecraft 26.2.
 *
 * Blur fix: never call extractBlurredBackground; solid fill only in extractBackground.
 * isInGameUi() true so vanilla background path cannot re-enable blur.
 * Drawing uses GuiGraphicsExtractor.fill / .text (not legacy drawString).
 */
public class ClickGUIScreen extends Screen {
    private final Map<Category, Panel> panels = new HashMap<>();
    private final List<Panel> panelList = new ArrayList<>();
    private Module bindingModule = null;

    private static final int BG = 0xCC0A0B0F;
    private static final int PANEL_BG = 0xF010131A;
    private static final int HEADER_BG = 0xFF16201C;
    private static final int ACCENT = 0xFF00E8A0;
    private static final int TEXT_DIM = 0xFF8B95A8;
    private static final int ROW_ON = 0xFF00E8A0;
    private static final int ROW_OFF = 0xFF8B95A8;

    public ClickGUIScreen() {
        super(Component.literal("TT Client"));
        int x = 12;
        for (Category cat : Category.values()) {
            Panel p = new Panel(cat, x, 28);
            panels.put(cat, p);
            panelList.add(p);
            x += 118;
        }
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Solid dim only — no extractTransparentBackground, no blur
        graphics.fill(0, 0, this.width, this.height, BG);
    }

    @Override
    protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {
        // intentionally empty
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        String header = "TT Client v" + TTClient.VERSION + "  |  Right Shift close  |  LMB toggle  RMB settings";
        graphics.text(this.font, header, 12, 8, TEXT_DIM, false);

        for (Panel panel : panelList) {
            panel.render(graphics, mouseX, mouseY);
        }

        if (bindingModule != null) {
            String msg = "Binding: " + bindingModule.getName() + "  (ESC clear)";
            int tw = this.font.width(msg);
            graphics.fill(this.width / 2 - tw / 2 - 8, this.height - 28, this.width / 2 + tw / 2 + 8, this.height - 12, 0xEE000000);
            graphics.text(this.font, msg, this.width / 2 - tw / 2, this.height - 24, ACCENT, false);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
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
        private final int width = 110;
        private final int headerHeight = 16;
        private Module expanded = null;

        public Panel(Category category, double x, double y) {
            this.category = category;
            this.x = x;
            this.y = y;
        }

        public void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
            int ix = (int) x;
            int iy = (int) y;

            int bodyH = headerHeight;
            List<Module> mods = TTClient.modules != null
                    ? TTClient.modules.getModulesByCategory(category)
                    : List.of();
            if (open) {
                for (Module mod : mods) {
                    bodyH += 14;
                    if (expanded == mod) bodyH += mod.getSettings().size() * 13;
                }
            }

            g.fill(ix, iy, ix + width, iy + bodyH, PANEL_BG);
            g.fill(ix, iy, ix + width, iy + headerHeight, HEADER_BG);
            g.fill(ix, iy, ix + 2, iy + bodyH, ACCENT);
            g.text(font, category.name, ix + 6, iy + 4, ACCENT, false);

            if (!open) return;

            int my = iy + headerHeight;
            for (Module mod : mods) {
                boolean on = mod.isEnabled();
                int color = on ? ROW_ON : ROW_OFF;
                if (mouseX >= ix && mouseX <= ix + width && mouseY >= my && mouseY <= my + 14) {
                    g.fill(ix + 2, my, ix + width, my + 14, 0x22FFFFFF);
                }
                g.text(font, mod.getName(), ix + 6, my + 3, color, false);
                if (on) {
                    g.fill(ix + width - 10, my + 5, ix + width - 5, my + 10, ACCENT);
                }
                my += 14;

                if (expanded == mod) {
                    for (Setting<?> s : mod.getSettings()) {
                        String label = s.getName() + ": " + String.valueOf(s.get());
                        g.text(font, label, ix + 10, my + 2, TEXT_DIM, false);
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
            if (!open || TTClient.modules == null) return false;

            List<Module> mods = TTClient.modules.getModulesByCategory(category);
            int my = (int) y + headerHeight;
            for (Module mod : mods) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= my && mouseY <= my + 14) {
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
                my += 14;
                if (expanded == mod) {
                    for (Setting<?> s : mod.getSettings()) {
                        if (mouseX >= x + 2 && mouseX <= x + width - 2 && mouseY >= my && mouseY <= my + 13) {
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
                        my += 13;
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
