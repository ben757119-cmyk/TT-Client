package com.ttclient.mixin;

import com.ttclient.client.ClientCommands;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Soft tab-complete for {@code .} commands — reflection only, no @Shadow. */
@Mixin(ChatScreen.class)
public abstract class MixinChatScreen {

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void ttclient$keyEvent(net.minecraft.client.input.KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (event == null) return;
            if (event.key() != GLFW.GLFW_KEY_TAB) return;
            if (complete((ChatScreen) (Object) this)) cir.setReturnValue(true);
        } catch (Throwable ignored) {}
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void ttclient$keyLegacy(int key, int scan, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (key != GLFW.GLFW_KEY_TAB) return;
            if (complete((ChatScreen) (Object) this)) cir.setReturnValue(true);
        } catch (Throwable ignored) {}
    }

    @Inject(method = "keyPressed", at = @At("RETURN"), cancellable = true, require = 0)
    private void ttclient$keyEventReturn(net.minecraft.client.input.KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (event == null || event.key() != GLFW.GLFW_KEY_TAB) return;
            if (Boolean.TRUE.equals(cir.getReturnValue())) return;
            if (complete((ChatScreen) (Object) this)) cir.setReturnValue(true);
        } catch (Throwable ignored) {}
    }

    private static boolean complete(ChatScreen screen) {
        try {
            Object box = findInput(screen);
            if (box == null) return false;
            Object raw = null;
            try { raw = box.getClass().getMethod("getValue").invoke(box); } catch (Throwable ignored) {}
            if (raw == null) {
                try { raw = box.getClass().getMethod("getText").invoke(box); } catch (Throwable ignored) {}
            }
            String val = raw == null ? null : String.valueOf(raw);
            if (val == null || !val.startsWith(ClientCommands.PREFIX)) return false;
            String completed = ClientCommands.tabComplete(val);
            if (completed == null || completed.equals(val)) return false;
            boolean set = false;
            try {
                box.getClass().getMethod("setValue", String.class).invoke(box, completed);
                set = true;
            } catch (Throwable ignored) {}
            if (!set) {
                try {
                    box.getClass().getMethod("setText", String.class).invoke(box, completed);
                    set = true;
                } catch (Throwable ignored) {}
            }
            if (!set) return false;
            try { box.getClass().getMethod("setCursorPosition", int.class).invoke(box, completed.length()); }
            catch (Throwable ignored) {
                try { box.getClass().getMethod("moveCursorToEnd", boolean.class).invoke(box, false); }
                catch (Throwable ignored2) {}
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object findInput(Object screen) {
        Class<?> c = screen.getClass();
        while (c != null && c != Object.class) {
            for (var f : c.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object v = f.get(screen);
                    if (v == null) continue;
                    String cn = v.getClass().getName().toLowerCase();
                    if (cn.contains("editbox") || cn.contains("textfield") || cn.contains("textbox")) return v;
                } catch (Throwable ignored) {}
            }
            c = c.getSuperclass();
        }
        try {
            for (var m : screen.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String n = m.getName().toLowerCase();
                if (!(n.contains("input") || n.contains("edit") || n.contains("field") || n.equals("getchatinput"))) continue;
                Object v = m.invoke(screen);
                if (v == null) continue;
                String cn = v.getClass().getName().toLowerCase();
                if (cn.contains("editbox") || cn.contains("textfield") || cn.contains("textbox")) return v;
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
