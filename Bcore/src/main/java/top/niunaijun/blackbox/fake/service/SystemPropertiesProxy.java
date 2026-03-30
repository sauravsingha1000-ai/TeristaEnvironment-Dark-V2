package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.fake.hook.ProxyMethods;
import top.niunaijun.blackbox.utils.Slog;

/**
 * SystemPropertiesProxy — intercepts android.os.SystemProperties.get/getInt/getBoolean.
 *
 * FIXES APPLIED:
 *   1. Added null-guard on getWho() — if SystemProperties is somehow not found
 *      we return null gracefully (ClassInvocationStub already handles null).
 *   2. Extended coverage to getInt(), getLong(), getBoolean() variants.
 *   3. Made property table complete and consistent with VirtualSpoof.cpp / FileSystemProxy.
 *   4. Two-argument form of get(key, default) now handled correctly.
 */
public class SystemPropertiesProxy extends ClassInvocationStub {

    private static final String TAG = "SystemPropertiesProxy";

    // Shared spoof table — consistent with VirtualSpoof.cpp and FileSystemProxy
    private static final Map<String, String> PROPS = new HashMap<>();
    static {
        PROPS.put("ro.debuggable",             "1");
        PROPS.put("ro.secure",                 "0");
        PROPS.put("service.adb.root",          "1");
        PROPS.put("ro.build.tags",             "release-keys");
        PROPS.put("ro.build.type",             "user");
        PROPS.put("ro.kernel.qemu",            "0");
        PROPS.put("ro.boot.qemu",              "0");
        PROPS.put("ro.hardware.egl",           "adreno");
        PROPS.put("ro.product.model",          "Pixel 6");
        PROPS.put("ro.product.brand",          "google");
        PROPS.put("ro.product.device",         "oriole");
        PROPS.put("ro.product.manufacturer",   "Google");
        PROPS.put("ro.product.board",          "lahaina");
        PROPS.put("ro.product.cpu.abi",        "arm64-v8a");
        PROPS.put("ro.hardware",               "qcom");
        PROPS.put("ro.boot.hardware",          "qcom");
        PROPS.put("ro.build.version.release",  "13");
        PROPS.put("ro.build.version.security_patch", "2023-01-05");
        PROPS.put("ro.serialno",               "1A2B3C4D5E6F");
        PROPS.put("ro.build.fingerprint",
                "google/oriole/oriole:13/TP1A.220624.014/8819:user/release-keys");
        PROPS.put("ro.build.version.sdk",      "33");
    }

    @Override
    protected Object getWho() {
        try {
            return Class.forName("android.os.SystemProperties");
        } catch (Exception e) {
            Slog.w(TAG, "android.os.SystemProperties not found: " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        // android.os.SystemProperties is a static-only class; there's no instance
        // field to replace. The proxy is installed so method calls are intercepted
        // via the InvocationHandler mechanism when code goes through the hook layer.
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // ── get(key) ─────────────────────────────────────────────────────────────

    @ProxyMethod("get")
    public static class Get extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args == null || args.length == 0 || !(args[0] instanceof String)) {
                return method.invoke(who, args);
            }
            String key = (String) args[0];
            String spoofed = PROPS.get(key);
            if (spoofed != null) return spoofed;
            // Two-arg form: get(key, default)
            if (args.length >= 2 && args[1] instanceof String) {
                return method.invoke(who, args);
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return "";
            }
        }
    }

    // ── getInt(key, def) ─────────────────────────────────────────────────────

    @ProxyMethod("getInt")
    public static class GetInt extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args == null || !(args[0] instanceof String)) return method.invoke(who, args);
            String key = (String) args[0];
            String spoofed = PROPS.get(key);
            if (spoofed != null) {
                try { return Integer.parseInt(spoofed); } catch (NumberFormatException ignored) {}
            }
            try { return method.invoke(who, args); } catch (Throwable e) { return 0; }
        }
    }

    // ── getLong(key, def) ────────────────────────────────────────────────────

    @ProxyMethod("getLong")
    public static class GetLong extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args == null || !(args[0] instanceof String)) return method.invoke(who, args);
            String key = (String) args[0];
            String spoofed = PROPS.get(key);
            if (spoofed != null) {
                try { return Long.parseLong(spoofed); } catch (NumberFormatException ignored) {}
            }
            try { return method.invoke(who, args); } catch (Throwable e) { return 0L; }
        }
    }

    // ── getBoolean(key, def) ─────────────────────────────────────────────────

    @ProxyMethod("getBoolean")
    public static class GetBoolean extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args == null || !(args[0] instanceof String)) return method.invoke(who, args);
            String key = (String) args[0];
            String spoofed = PROPS.get(key);
            if (spoofed != null) return "1".equals(spoofed) || "true".equalsIgnoreCase(spoofed);
            try { return method.invoke(who, args); } catch (Throwable e) { return false; }
        }
    }
}
