package com.ttclient.mixin;

import com.ttclient.client.ClientCommands;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tab-complete TT client commands starting with {@code .} */
@Mixin(ChatScreen.class)
public abstract class MixinChatScreen {

    @Shadow
    protected EditBox input;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void ttclient$tabComplete(int key, int scan, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (key != GLFW.GLFW_KEY_TAB) return;
        try {
            if (input == null) return;
            String val = input.getValue();
            if (val == null || !val.startsWith(ClientCommands.PREFIX)) return;
            String completed = ClientCommands.tabComplete(val);
            if (completed != null && !completed.equals(val)) {
                input.setValue(completed);
                try { input.setCursorPosition(completed.length()); } catch (Throwable ignored) {}
                try { input.setHighlightPos(completed.length()); } catch (Throwable ignored) {}
                cir.setReturnValue(true);
            }
        } catch (Throwable ignored) {}
    }
}
