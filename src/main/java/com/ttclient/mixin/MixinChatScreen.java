package com.ttclient.mixin;

import com.ttclient.client.ClientCommands;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Soft tab-complete for {@code .} commands. No @Shadow — finds the input field by type via reflection.
 */
@Mixin(ChatScreen.class)
public abstract class MixinChatScreen {

    @Inject(method = {"keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z", "keyPressed(III)Z", "keyPressed"},
            at = @At("HEAD"), cancellable = true, require = 0)
    private void ttclient$tab(CallbackInfoReturnable<Boolean> cir, org.spongepowered.asm.mixin.injection.callback.CallbackInfo unused) {
        // placeholder — real injects below with proper signatures
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void ttclient$keyPressedAny(CallbackInfoReturnable<Boolean> cir) {
        // Can't read key from no-arg; handled by specific overloads
    }

    // Modern 26.2 style KeyEvent
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void ttclient$keyEvent(net.minecraft.client.input.KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        try {
            int key = event.key();
            if (key != GLFW.GLFW_KEY_TAB) return;
            if (tryComplete((ChatScreen) (Object) this))
                cir.setReturnValue(true);
        } catch (Throwable ignored) {}
    }

    // Legacy int,int,int
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void ttclient$keyLegacy(int key, int scan, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (key != GLFW.GLFW_KEY_TAB) return;
            if (tryComplete((ChatScreen) (Object) this))
                cir.setReturnValue(true);
        } catch (Throwable ignored) {}
    }

    private static boolean tryComplete(ChatScreen screen) {
        try {
            Object box = findInput(screen);
            if (box == null) return false;
            String val = String.valueOf(box.getClass().getMethod("getValue").invoke(box));
            if (val == null || !val.startsWith(ClientCommands.PREFIX)) return false;
            String completed = ClientCommands.tabComplete(val);
            if (completed == null || completed.equals(val)) return false;
            box.getClass().getMethod("setValue", String.class).invoke(box, completed);
            try {
                box.getClass().getMethod("setCursorPosition", int.class).invoke(box, completed.length());
            } catch (Throwable ignored) {}
            try {
                box.getClass().getMethod("moveCursorToEnd", boolean.class).invoke(box, false);
            } catch (Throwable ignored) {}
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object findInput(Object screen) {
        try {
            for (var f : screen.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object v = f.get(screen);
                if (v == null) continue;
                String cn = v.getClass().getName();
                if (cn.contains("EditBox") || cn.contains("TextField") || cn.contains("editbox")) {
                    return v;
                }
            }
            // superclass fields
            Class<?> c = screen.getClass().getSuperclass();
            while (c != null && c != Object.class) {
                for (var f : c.getDeclaredFields()) {
                    f.setAccessible(true);
                    Object v = f.get(screen);
                    if (v == null) continue;
                    String cn = v.getClass().getName();
                    if (cn.contains("EditBox") || cn.contains("TextField")) return v;
                }
                c = c.getSuperclass();
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
