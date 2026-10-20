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

    private static boolean complete(ChatScreen screen) {
        try {
            Object box = findInput(screen);
            if (box == null) return false;
            Object raw = box.getClass().getMethod("getValue").invoke(box);
            String val = raw == null ? null : String.valueOf(raw);
            if (val == null || !val.startsWith(ClientCommands.PREFIX)) return false;
            String completed = ClientCommands.tabComplete(val);
            if (completed == null || completed.equals(val)) return false;
            box.getClass().getMethod("setValue", String.class).invoke(box, completed);
            try { box.getClass().getMethod("setCursorPosition", int.class).invoke(box, completed.length()); }
            catch (Throwable ignored) {}
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
                    if (cn.contains("editbox") || cn.contains("textfield")) return v;
                } catch (Throwable ignored) {}
            }
            c = c.getSuperclass();
        }
        return null;
    }
}
