package com.ttclient.gui;

import com.ttclient.TTClient;
import com.ttclient.client.TTClientClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.modules.client.ClickGUIModule;
import com.ttclient.modules.render.BlockESP;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ModeSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.settings.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClickGUIScreen extends Screen {
    private final Map<Category, Panel> panels = new HashMap<>();
    private final List<Panel> panelList = new ArrayList<>();
    private Module bindingModule = null;
    private Module hoveredModule = null;
    private String searchQuery = "";
    private boolean searchFocused = true;
    private NumberSetting sliding = null;
    private int sliderTrackX, sliderTrackW;
    private NumberSetting typing = null;
    private String typingBuffer = "";

    private static final int PANEL_BG = 0xF00B0D12, HEADER_BG = 0xFF080A0E, TEXT_DIM = 0xFF6B7280, ROW_OFF = 0xFF9CA3AF;
    private static final int SLIDER_BG = 0xFF141820, SEARCH_BG = 0xF0080A0E, ROW_ON_BG = 0x2200E8A0;
    private static final int ROW_H = 14, SETTING_H = 12, NUMBER_H = 26;
    private static final int SEARCH_X = 10, SEARCH_Y = 6, SEARCH_W = 168, SEARCH_H = 15;
    private final List<Particle> particles = new ArrayList<>();

    private static void roundedFill(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int r, int color) {
        if (r < 1) { g.fill(x1, y1, x2, y2, color); return; }
        r = Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        g.fill(x1 + r, y1, x2 - r, y2, color);
        g.fill(x1, y1 + r, x2, y2 - r, color);
        g.fill(x1 + 1, y1 + 1, x1 + r, y1 + r, color);
        g.fill(x2 - r, y1 + 1, x2 - 1, y1 + r, color);
        g.fill(x1 + 1, y2 - r, x1 + r, y2 - 1, color);
        g.fill(x2 - r, y2 - r, x2 - 1, y2 - 1, color);
    }

    private static final class Particle {
        float x, y, vy, size;
        Particle(float x, float y) {
            this.x = x; this.y = y;
            this.vy = 0.25f + (float) Math.random() * 0.85f;
            this.size = 1 + (float) Math.random() * 2;
        }
    }

    private int accent() {
        ClickGUIModule guiMod = TTClientClient.modules != null ? TTClientClient.modules.getModule(ClickGUIModule.class) : null;
        return guiMod != null ? guiMod.accent.get() : 0xFF00E8A0;
    }
    private static com.ttclient.modules.ModuleManager manager() {
        return TTClientClient.modules != null ? TTClientClient.modules : TTClient.modules;
    }

    public ClickGUIScreen() {
        super(Component.literal("TT Client"));
        int x = 10;
        for (Category cat : Category.values()) {
            if (manager() != null && manager().getModulesByCategory(cat).isEmpty()) continue;
            Panel p = new Panel(cat, x, 36);
            panels.put(cat, p);
            panelList.add(p);
            x += 108;
        }
    }

    @Override public boolean isInGameUi() { return true; }
    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x99050A12);
        graphics.fill(0, 0, this.width, this.height, 0x44060C18);
        if (particles.isEmpty() && this.width > 0) {
            for (int i = 0; i < 40; i++)
                particles.add(new Particle((float) (Math.random() * this.width), (float) (Math.random() * this.height)));
        }
        for (Particle p : particles) {
            p.y += p.vy;
            if (p.y > this.height) { p.y = -4; p.x = (float) (Math.random() * Math.max(1, this.width)); }
            int s = Math.max(1, (int) p.size);
            graphics.fill((int) p.x, (int) p.y, (int) p.x + s, (int) p.y + s, 0x5500E8A0);
        }
    }

    @Override protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {
        try { super.extractBlurredBackground(graphics); } catch (Throwable ignored) {}
    }
    @Override public void extractTransparentBackground(GuiGraphicsExtractor graphics) {}
    @Override protected void extractMenuBackground(GuiGraphicsExtractor graphics) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        hoveredModule = null;
        roundedFill(graphics, SEARCH_X, SEARCH_Y, SEARCH_X + SEARCH_W, SEARCH_Y + SEARCH_H, 4, SEARCH_BG);
        graphics.fill(SEARCH_X, SEARCH_Y, SEARCH_X + 2, SEARCH_Y + SEARCH_H, accent());
        String shown = searchQuery.isEmpty() ? (searchFocused ? "search..." : "search modules") : searchQuery + (searchFocused ? "_" : "");
        graphics.text(this.font, shown, SEARCH_X + 8, SEARCH_Y + 4, searchQuery.isEmpty() ? TEXT_DIM : 0xFFE8EAED, false);
        graphics.text(this.font, "tt  " + TTClient.VERSION, SEARCH_X + SEARCH_W + 10, SEARCH_Y + 4, accent(), false);

        if (searchQuery != null && !searchQuery.isBlank() && manager() != null) {
            String q = searchQuery.toLowerCase(Locale.ROOT);
            int sy = SEARCH_Y + SEARCH_H + 2;
            int shownN = 0;
            for (Module m : manager().getModules()) {
                if (!m.getName().toLowerCase(Locale.ROOT).contains(q)
                        && !m.getDescription().toLowerCase(Locale.ROOT).contains(q)) continue;
                roundedFill(graphics, SEARCH_X, sy, SEARCH_X + SEARCH_W, sy + 13, 3, 0xF010141C);
                graphics.fill(SEARCH_X, sy, SEARCH_X + 2, sy + 13, m.isEnabled() ? accent() : 0xFF334155);
                graphics.text(this.font, m.getName(), SEARCH_X + 8, sy + 3, m.isEnabled() ? accent() : 0xFFE8EAED, false);
                sy += 13;
                if (++shownN >= 10) break;
            }
            if (shownN == 0) {
                roundedFill(graphics, SEARCH_X, sy, SEARCH_X + SEARCH_W, sy + 13, 3, 0xF010141C);
                graphics.text(this.font, "no modules", SEARCH_X + 8, sy + 3, TEXT_DIM, false);
            }
        }

        for (Panel panel : panelList) panel.render(graphics, mouseX, mouseY);
        if (hoveredModule != null && typing == null) {
            String title = hoveredModule.getName();
            String desc = hoveredModule.getDescription();
            int pad = 8, tw = Math.max(font.width(title), font.width(desc)), boxW = tw + pad * 2, boxH = 26;
            int tx = Math.min(mouseX + 12, this.width - boxW - 4), ty = Math.min(mouseY + 12, this.height - boxH - 4);
            roundedFill(graphics, tx, ty, tx + boxW, ty + boxH, 4, 0xF0080A0E);
            graphics.fill(tx, ty, tx + 2, ty + boxH, accent());
            graphics.text(font, title, tx + pad, ty + 4, accent(), false);
            graphics.text(font, desc, tx + pad, ty + 14, TEXT_DIM, false);
        }
        if (bindingModule != null) {
            String msg = "bind  " + bindingModule.getName() + "  ·  esc clear";
            int tw = this.font.width(msg);
            roundedFill(graphics, this.width / 2 - tw / 2 - 12, this.height - 30, this.width / 2 + tw / 2 + 12, this.height - 12, 4, 0xF0080A0E);
            graphics.text(this.font, msg, this.width / 2 - tw / 2, this.height - 24, accent(), false);
        }
    }

    @Override public void onClose() {
        commitTyping(true); sliding = null;
        for (Panel p : panelList) if (p.expanded instanceof BlockESP be) be.uiSearchFocused = false;
        super.onClose();
        if (TTClientClient.config != null) TTClientClient.config.save();
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y(); int button = event.button();
        if (bindingModule != null && button >= 0) {
            bindingModule.setKeyBind(2000 + button);
            bindingModule = null;
            return true;
        }
        if (button == 0 && mouseX >= SEARCH_X && mouseX <= SEARCH_X + SEARCH_W && mouseY >= SEARCH_Y && mouseY <= SEARCH_Y + SEARCH_H) {
            searchFocused = true; commitTyping(true);
            for (Panel p : panelList) if (p.expanded instanceof BlockESP be) be.uiSearchFocused = false;
            return true;
        }
        if (button == 0 && searchQuery != null && !searchQuery.isBlank() && manager() != null
                && mouseX >= SEARCH_X && mouseX <= SEARCH_X + SEARCH_W && mouseY > SEARCH_Y + SEARCH_H) {
            String q = searchQuery.toLowerCase(Locale.ROOT);
            int sy = SEARCH_Y + SEARCH_H + 2;
            for (Module m : manager().getModules()) {
                if (!m.getName().toLowerCase(Locale.ROOT).contains(q)
                        && !m.getDescription().toLowerCase(Locale.ROOT).contains(q)) continue;
                if (mouseY >= sy && mouseY <= sy + 13) { m.toggle(); return true; }
                sy += 13;
                if (sy > SEARCH_Y + SEARCH_H + 2 + 13 * 10) break;
            }
        }
        if (button == 0) searchFocused = false;
        for (int i = panelList.size() - 1; i >= 0; i--) if (panelList.get(i).mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(event, doubleClick);
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        sliding = null; for (Panel panel : panelList) panel.mouseReleased(); return super.mouseReleased(event);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (sliding != null && sliderTrackW > 0) { applySlider(sliding, event.x(), sliderTrackX, sliderTrackW); return true; }
        for (Panel panel : panelList) if (panel.mouseDragged(event.x(), event.y())) return true;
        return super.mouseDragged(event, dragX, dragY);
    }

    private void applySlider(NumberSetting ns, double mouseX, int trackX, int trackW) {
        double t = Math.max(0, Math.min(1, (mouseX - trackX) / (double) trackW));
        ns.set(ns.getMin() + t * (ns.getMax() - ns.getMin()));
    }
    private void startTyping(NumberSetting ns) { commitTyping(true); typing = ns; typingBuffer = ns.getDisplay(); sliding = null; searchFocused = false; }
    private void commitTyping(boolean apply) {
        if (typing == null) return;
        if (apply) { try { String raw = typingBuffer.trim().replace(',', '.'); if (!raw.isEmpty()) typing.set(Double.parseDouble(raw)); } catch (NumberFormatException ignored) {} }
        typing = null; typingBuffer = "";
    }

    @Override public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (typing == null && bindingModule == null) {
            for (Panel panel : panelList) {
                if (panel.expanded instanceof BlockESP be && be.uiSearchFocused) {
                    if (keyCode == GLFW.GLFW_KEY_ESCAPE) { be.uiSearchFocused = false; return true; }
                    if (keyCode == GLFW.GLFW_KEY_BACKSPACE && be.uiSearch != null && !be.uiSearch.isEmpty()) {
                        be.uiSearch = be.uiSearch.substring(0, be.uiSearch.length() - 1); return true;
                    }
                    if (be.uiSearch == null) be.uiSearch = "";
                    if (be.uiSearch.length() < 40) {
                        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) { be.uiSearch += (char)('a'+(keyCode-GLFW.GLFW_KEY_A)); return true; }
                        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) { be.uiSearch += (char)('0'+(keyCode-GLFW.GLFW_KEY_0)); return true; }
                        if (keyCode == GLFW.GLFW_KEY_MINUS) { be.uiSearch += '_'; return true; }
                    }
                    return true;
                }
            }
        }
        if (typing != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { commitTyping(true); return true; }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { commitTyping(false); return true; }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !typingBuffer.isEmpty()) { typingBuffer = typingBuffer.substring(0, typingBuffer.length()-1); return true; }
            if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) { typingBuffer += (char)('0'+(keyCode-GLFW.GLFW_KEY_0)); return true; }
            if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) { typingBuffer += (char)('0'+(keyCode-GLFW.GLFW_KEY_KP_0)); return true; }
            if (keyCode == GLFW.GLFW_KEY_PERIOD || keyCode == GLFW.GLFW_KEY_KP_DECIMAL) { if (!typingBuffer.contains(".")) typingBuffer += '.'; return true; }
            return true;
        }
        if (bindingModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) bindingModule.setKeyBind(-1);
            else if (keyCode > 0) bindingModule.setKeyBind(keyCode);
            bindingModule = null; return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) { onClose(); return true; }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) { searchQuery = searchQuery.substring(0, searchQuery.length()-1); searchFocused = true; return true; }
        if (searchQuery.length() < 32) {
            if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) { searchQuery += (char)('a'+(keyCode-GLFW.GLFW_KEY_A)); searchFocused = true; return true; }
            if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) { searchQuery += (char)('0'+(keyCode-GLFW.GLFW_KEY_0)); searchFocused = true; return true; }
            if (keyCode == GLFW.GLFW_KEY_SPACE) { searchQuery += ' '; searchFocused = true; return true; }
        }
        return super.keyPressed(event);
    }

    public void startBinding(Module module) { commitTyping(true); this.bindingModule = module; searchFocused = false; }
    private static String keyName(int key) {
        if (key <= 0) return "NONE";
        if (key >= 2000 && key < 2016) {
            int b = key - 2000;
            if (b == GLFW.GLFW_MOUSE_BUTTON_4) return "MOUSE4";
            if (b == GLFW.GLFW_MOUSE_BUTTON_5) return "MOUSE5";
            if (b == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) return "MOUSE3";
            return "MOUSE" + (b + 1);
        }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) return "RSHIFT";
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) return String.valueOf((char)('A'+(key-GLFW.GLFW_KEY_A)));
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) return String.valueOf((char)('0'+(key-GLFW.GLFW_KEY_0)));
        return "KEY"+key;
    }
    private int settingRowHeight(Setting<?> s) { return s instanceof NumberSetting ? NUMBER_H : SETTING_H; }

    public class Panel {
        private final Category category;
        private double x, y;
        private boolean open = true, dragging = false;
        private double dragOffsetX, dragOffsetY;
        private final int width = 102, headerHeight = 16;
        Module expanded = null;

        public Panel(Category category, double x, double y) { this.category = category; this.x = x; this.y = y; }

        private List<Module> filteredModules() {
            List<Module> base = manager() != null ? manager().getModulesByCategory(category) : List.of();
            if (searchQuery == null || searchQuery.isBlank()) return base;
            String q = searchQuery.toLowerCase(Locale.ROOT);
            List<Module> out = new ArrayList<>();
            for (Module m : base) if (m.getName().toLowerCase(Locale.ROOT).contains(q) || m.getDescription().toLowerCase(Locale.ROOT).contains(q)) out.add(m);
            return out;
        }

        private int bodyHeight(List<Module> mods) {
            int bodyH = headerHeight;
            if (!open) return bodyH;
            for (Module mod : mods) {
                bodyH += ROW_H;
                if (expanded == mod) {
                    for (Setting<?> s : mod.getSettings()) bodyH += settingRowHeight(s);
                    bodyH += SETTING_H;
                    if (mod instanceof BlockESP be) {
                        bodyH += SETTING_H;
                        bodyH += SETTING_H;
                        bodyH += Math.min(5, be.searchRegistry(be.uiSearch, 5).size()) * SETTING_H;
                        bodyH += SETTING_H;
                        bodyH += Math.min(6, be.getSelected().size()) * SETTING_H;
                    }
                }
            }
            return bodyH;
        }

        public void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
            int ix = (int) x, iy = (int) y;
            List<Module> mods = filteredModules();
            int bodyH = bodyHeight(mods);
            roundedFill(g, ix + 2, iy + 2, ix + width + 2, iy + bodyH + 2, 6, 0x44000000);
            roundedFill(g, ix, iy, ix + width, iy + bodyH, 6, PANEL_BG);
            roundedFill(g, ix, iy, ix + width, iy + headerHeight, 6, HEADER_BG);
            g.fill(ix, iy + headerHeight - 4, ix + width, iy + headerHeight, HEADER_BG);
            g.fill(ix, iy + headerHeight - 1, ix + width, iy + headerHeight, 0x3300E8A0);
            g.fill(ix, iy + 4, ix + 2, iy + bodyH - 4, accent());
            g.text(font, category.name.toUpperCase(Locale.ROOT), ix + 8, iy + 4, accent(), false);
            g.text(font, open ? "-" : "+", ix + width - 12, iy + 4, TEXT_DIM, false);
            if (!open) return;
            int my = iy + headerHeight;
            for (Module mod : mods) {
                boolean on = mod.isEnabled();
                int color = on ? accent() : ROW_OFF;
                if (mouseX >= ix && mouseX <= ix + width && mouseY >= my && mouseY <= my + ROW_H) {
                    g.fill(ix + 2, my, ix + width - 2, my + ROW_H, 0x14FFFFFF);
                    hoveredModule = mod;
                }
                if (on) g.fill(ix + 2, my, ix + width - 2, my + ROW_H, ROW_ON_BG);
                g.text(font, mod.getName(), ix + 8, my + 3, color, false);
                if (on) g.fill(ix + width - 10, my + 4, ix + width - 5, my + 9, accent());
                my += ROW_H;
                if (expanded == mod) {
                    for (Setting<?> s : mod.getSettings()) {
                        if (s instanceof NumberSetting ns) {
                            g.text(font, ns.getName(), ix + 8, my + 2, TEXT_DIM, false);
                            String valText = (typing == ns) ? typingBuffer + "_" : ns.getDisplay();
                            int vw = font.width(valText);
                            g.text(font, valText, ix + width - 8 - vw, my + 2, typing == ns ? accent() : 0xFFE8EAED, false);
                            int trackX = ix + 8, trackW = width - 16, trackY = my + 14;
                            g.fill(trackX, trackY, trackX + trackW, trackY + 3, SLIDER_BG);
                            double range = ns.getMax() - ns.getMin();
                            double t = range <= 0 ? 0 : (ns.get() - ns.getMin()) / range;
                            int fillW = (int) Math.round(trackW * Math.max(0, Math.min(1, t)));
                            if (fillW > 0) g.fill(trackX, trackY, trackX + fillW, trackY + 3, 0xFF0A3D32);
                            g.fill(trackX + Math.max(0, fillW - 1), trackY - 1, trackX + fillW + 1, trackY + 4, accent());
                            my += NUMBER_H;
                        } else {
                            g.text(font, s.getName() + ": " + String.valueOf(s.get()), ix + 8, my + 2, TEXT_DIM, false);
                            my += SETTING_H;
                        }
                    }
                    g.text(font, "bind  " + keyName(mod.getKeyBind()), ix + 8, my + 2, accent(), false);
                    my += SETTING_H;
                    if (mod instanceof BlockESP be) {
                        g.text(font, ".b add <block>", ix + 8, my + 2, TEXT_DIM, false);
                        my += SETTING_H;
                        String q = be.uiSearch == null ? "" : be.uiSearch;
                        boolean foc = be.uiSearchFocused;
                        g.fill(ix + 6, my, ix + width - 6, my + SETTING_H - 1, foc ? 0xFF1A2A24 : 0xFF12151C);
                        String sShown = q.isEmpty() ? (foc ? "block id..." : "search") : q + (foc ? "_" : "");
                        g.text(font, sShown, ix + 8, my + 2, foc ? accent() : TEXT_DIM, false);
                        my += SETTING_H;
                        for (String id : be.searchRegistry(q, 5)) {
                            String shortId = id.startsWith("minecraft:") ? id.substring(10) : id;
                            if (shortId.length() > 12) shortId = shortId.substring(0, 10) + "..";
                            g.text(font, "+ " + shortId, ix + 8, my + 2, 0xFF8BFFB0, false);
                            my += SETTING_H;
                        }
                        g.text(font, "selected", ix + 8, my + 2, TEXT_DIM, false);
                        my += SETTING_H;
                        for (String id : be.getSelected()) {
                            String shortId = id.startsWith("minecraft:") ? id.substring(10) : id;
                            if (shortId.length() > 12) shortId = shortId.substring(0, 10) + "..";
                            g.text(font, "- " + shortId, ix + 8, my + 2, 0xFFFF8888, false);
                            my += SETTING_H;
                        }
                    }
                }
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight) {
                if (button == 0) { dragging = true; dragOffsetX = mouseX - x; dragOffsetY = mouseY - y; return true; }
                if (button == 1) { open = !open; return true; }
            }
            if (!open || manager() == null) return false;
            List<Module> mods = filteredModules();
            int my = (int) y + headerHeight, ix = (int) x;
            for (Module mod : mods) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= my && mouseY <= my + ROW_H) {
                    if (button == 0) { commitTyping(true); mod.toggle(); return true; }
                    if (button == 1) { expanded = expanded == mod ? null : mod; return true; }
                    if (button == 2) { startBinding(mod); return true; }
                }
                my += ROW_H;
                if (expanded == mod) {
                    for (Setting<?> s : mod.getSettings()) {
                        int h = settingRowHeight(s);
                        if (s instanceof NumberSetting ns && mouseX >= x && mouseX <= x + width && mouseY >= my && mouseY <= my + h) {
                            if (button == 0) {
                                if (mouseY >= my + 11) {
                                    sliding = ns; sliderTrackX = ix + 8; sliderTrackW = width - 16;
                                    applySlider(ns, mouseX, sliderTrackX, sliderTrackW);
                                } else startTyping(ns);
                                return true;
                            }
                        } else if (s instanceof BoolSetting bs && mouseX >= x && mouseX <= x + width && mouseY >= my && mouseY <= my + h && button == 0) {
                            bs.set(!bs.get()); return true;
                        } else if (s instanceof ModeSetting ms && mouseX >= x && mouseX <= x + width && mouseY >= my && mouseY <= my + h && button == 0) {
                            ms.cycle(); return true;
                        }
                        my += h;
                    }
                    if (mouseX >= x && mouseX <= x + width && mouseY >= my && mouseY <= my + SETTING_H && button == 0) {
                        startBinding(mod); return true;
                    }
                    my += SETTING_H;
                    if (mod instanceof BlockESP be) {
                        my += SETTING_H;
                        if (mouseX >= x && mouseX <= x + width && mouseY >= my && mouseY <= my + SETTING_H && button == 0) {
                            be.uiSearchFocused = true; searchFocused = false; return true;
                        }
                        my += SETTING_H;
                        for (String id : be.searchRegistry(be.uiSearch == null ? "" : be.uiSearch, 5)) {
                            if (mouseX >= x && mouseX <= x + width && mouseY >= my && mouseY <= my + SETTING_H && button == 0) {
                                be.addBlock(id); return true;
                            }
                            my += SETTING_H;
                        }
                        my += SETTING_H;
                        for (String id : be.getSelected()) {
                            if (mouseX >= x && mouseX <= x + width && mouseY >= my && mouseY <= my + SETTING_H && button == 0) {
                                be.removeBlock(id); return true;
                            }
                            my += SETTING_H;
                        }
                    }
                }
            }
            return false;
        }

        public void mouseReleased() { dragging = false; }
        public boolean mouseDragged(double mouseX, double mouseY) {
            if (dragging) { x = mouseX - dragOffsetX; y = mouseY - dragOffsetY; return true; }
            return false;
        }
    }
}
