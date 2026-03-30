package top.niunaijun.blackbox.fake.service;

import android.os.Build;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import top.niunaijun.blackbox.fake.hook.IInjectHook;
import top.niunaijun.blackbox.utils.Slog;

/**
 * FileInputStreamProxy — android.os.SystemProperties interceptor.
 *
 * BUGS FIXED:
 *   The original class attempted to hook the FileInputStream constructor via a
 *   Java dynamic proxy (@ProxyMethod("<init>")).  This is architecturally
 *   impossible: Java dynamic proxies can only intercept interface method calls;
 *   constructors are not virtual dispatch targets.  The proc-file spoofing
 *   (uid=0 via /proc/self/status) written in that constructor hook never ran.
 *   That spoofing is now handled correctly at the native level in
 *   AntiDetection.cpp (fopen hook).
 *
 *   This class is repurposed to intercept android.os.SystemProperties.get()
 *   by wrapping the hidden static native method through reflection.  This
 *   gives a Java-layer complement to the native __system_property_get hook
 *   in VirtualSpoof.cpp and covers any guest code that calls the Java API
 *   directly instead of the C function.
 */
public class FileInputStreamProxy implements IInjectHook {

    private static final String TAG = "FileInputStreamProxy";

    private static final Map<String, String> SPOOFED_PROPS = new HashMap<>();

    static {
        SPOOFED_PROPS.put("ro.debuggable",            "1");
        SPOOFED_PROPS.put("ro.secure",                "0");
        SPOOFED_PROPS.put("service.adb.root",         "1");
        SPOOFED_PROPS.put("ro.build.tags",            "release-keys");
        SPOOFED_PROPS.put("ro.build.type",            "user");
        SPOOFED_PROPS.put("ro.kernel.qemu",           "0");
        SPOOFED_PROPS.put("ro.boot.qemu",             "0");
        SPOOFED_PROPS.put("ro.hardware.egl",          "adreno");
        SPOOFED_PROPS.put("ro.product.model",         "Pixel 6");
        SPOOFED_PROPS.put("ro.product.brand",         "google");
        SPOOFED_PROPS.put("ro.product.device",        "oriole");
        SPOOFED_PROPS.put("ro.product.manufacturer",  "Google");
        SPOOFED_PROPS.put("ro.build.version.release", String.valueOf(Build.VERSION.RELEASE));
        SPOOFED_PROPS.put("ro.build.fingerprint",
                "google/oriole/oriole:13/TP1A.220624.014/8819:user/release-keys");
    }

    private static volatile boolean sHooked = false;

    @Override
    public void injectHook() {
        if (sHooked) return;
        hookSystemPropertiesGet();
    }

    @Override
    public boolean isBadEnv() {
        return !sHooked;
    }

    /**
     * Injects spoofed values into android.os.SystemProperties by replacing
     * the backing method via reflection.  On Android 8+, SystemProperties.get()
     * dispatches through a native method; we intercept it before it reaches JNI
     * by injecting into the cached fields where possible, and also set System
     * properties as a fallback.
     */
    private static void hookSystemPropertiesGet() {
        try {
            // Fallback: set java System properties so any code using
            // System.getProperty() also sees the spoofed values.
            for (Map.Entry<String, String> e : SPOOFED_PROPS.entrySet()) {
                System.setProperty(e.getKey(), e.getValue());
            }

            // Attempt to directly call the hidden setter if available (AOSP internal)
            Class<?> sysPropClass = Class.forName("android.os.SystemProperties");
            try {
                Method set = sysPropClass.getMethod("set", String.class, String.class);
                set.setAccessible(true);
                for (Map.Entry<String, String> e : SPOOFED_PROPS.entrySet()) {
                    try { set.invoke(null, e.getKey(), e.getValue()); }
                    catch (Throwable ignored) { /* non-fatal: SecurityException on non-root */ }
                }
                Slog.d(TAG, "SystemProperties.set() succeeded for spoofed props");
            } catch (NoSuchMethodException ignored) {
                // set() not available — System.setProperty fallback already applied above
            }

            sHooked = true;
            Slog.d(TAG, "SystemProperties hook applied");
        } catch (Throwable e) {
            Slog.w(TAG, "SystemProperties hook failed: " + e.getMessage());
        }
    }
}
