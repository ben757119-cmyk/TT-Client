package com.ttclient.client;

import com.ttclient.TTClient;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFWNativeWin32;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * Windows 10 2004+ stream-proofing: SetWindowDisplayAffinity(WDA_EXCLUDEFROMCAPTURE).
 * You still see the game; OBS / Discord / Win+G capture of this window will not.
 * No-op on non-Windows or if the API is unavailable.
 */
public final class CaptureExclude {
    private static final int WDA_NONE = 0x00;
    private static final int WDA_EXCLUDEFROMCAPTURE = 0x11;

    private static MethodHandle setWindowDisplayAffinity;
    private static boolean initTried;
    private static boolean supported;

    private CaptureExclude() {}

    public static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    public static boolean isSupported() {
        ensureInit();
        return supported;
    }

    private static void ensureInit() {
        if (initTried) return;
        initTried = true;
        if (!isWindows()) return;
        try {
            Linker linker = Linker.nativeLinker();
            SymbolLookup user32 = SymbolLookup.libraryLookup("user32", Arena.global());
            var sym = user32.find("SetWindowDisplayAffinity");
            if (sym.isEmpty()) return;
            setWindowDisplayAffinity = linker.downcallHandle(
                    sym.get(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT)
            );
            supported = true;
        } catch (Throwable t) {
            TTClient.LOGGER.warn("CaptureExclude unavailable: {}", t.toString());
            supported = false;
        }
    }

    public static long hwnd() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getWindow() == null) return 0L;
            long glfw = mc.getWindow().handle();
            if (glfw == 0L) return 0L;
            return GLFWNativeWin32.glfwGetWin32Window(glfw);
        } catch (Throwable t) {
            return 0L;
        }
    }

    /** @return true if the OS accepted the affinity change */
    public static boolean setExcludedFromCapture(boolean exclude) {
        ensureInit();
        if (!supported || setWindowDisplayAffinity == null) return false;
        long h = hwnd();
        if (h == 0L) return false;
        try {
            int affinity = exclude ? WDA_EXCLUDEFROMCAPTURE : WDA_NONE;
            int result = (int) setWindowDisplayAffinity.invokeExact(h, affinity);
            boolean ok = result != 0;
            if (ok) {
                TTClient.LOGGER.info("CaptureExclude {}", exclude ? "ON (hidden from capture)" : "OFF");
            }
            return ok;
        } catch (Throwable t) {
            TTClient.LOGGER.warn("CaptureExclude failed: {}", t.toString());
            return false;
        }
    }
}
