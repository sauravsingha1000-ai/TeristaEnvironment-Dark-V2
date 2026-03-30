package top.niunaijun.blackbox.fake.service;

import android.os.Build;

import java.lang.reflect.Field;

import top.niunaijun.blackbox.fake.hook.IInjectHook;
import top.niunaijun.blackbox.utils.Slog;

/**
 * FileSystemProxy — Java-level fake root environment bootstrap.
 *
 * BUGS FIXED:
 *   1. Original class extended ClassInvocationStub but returned null from getWho(),
 *      causing ClassInvocationStub.injectHook() to return on the first null-check.
 *      Every @ProxyMethod in the class was dead code and was never executed.
 *
 *   2. File existence hooks (exists, mkdirs, isDirectory) were attached to a
 *      ClassInvocationStub that never activated. Java dynamic proxies also cannot
 *      intercept final classes like java.io.File. The correct layer for file-path
 *      redirection and su-path faking is the native UnixFileSystemHook (fixed
 *      separately in UnixFileSystemHook.cpp).
 *
 * REPLACEMENT DESIGN:
 *   This class now implements IInjectHook directly and performs two concrete
 *   tasks that ARE possible at the Java layer:
 *     a) Spoof android.os.Build fields to look like a clean Google device.
 *     b) Set System properties for ro.debuggable / ro.secure so Java code
 *        reading System.getProperty() sees root-friendly values.
 *
 *   Native-layer spoofing (access/stat/fopen/getprop) is handled by
 *   AntiDetection.cpp and VirtualSpoof.cpp (already loaded via libblackbox.so).
 */
public class FileSystemProxy implements IInjectHook {

    private static final String TAG = "FileSystemProxy";

    // Realistic Pixel 6 fingerprint — consistent with VirtualSpoof.cpp
    private static final String FAKE_FINGERPRINT =
            "google/oriole/oriole:13/TP1A.220624.014/8819:user/release-keys";
    private static final String FAKE_MODEL        = "Pixel 6";
    private static final String FAKE_BRAND        = "google";
    private static final String FAKE_DEVICE       = "oriole";
    private static final String FAKE_MANUFACTURER = "Google";
    private static final String FAKE_TAGS         = "release-keys";
    private static final String FAKE_TYPE         = "user";

    @Override
    public void injectHook() {
        spoofBuildFields();
        spoofSystemProperties();
    }

    @Override
    public boolean isBadEnv() {
        // Re-inject if Build fields have been reset (e.g. by some OEM hook)
        try {
            Field f = Build.class.getDeclaredField("MODEL");
            f.setAccessible(true);
            String current = (String) f.get(null);
            return !FAKE_MODEL.equals(current);
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ── Build field spoofing ──────────────────────────────────────────────────

    private void spoofBuildFields() {
        try {
            setField(Build.class, "FINGERPRINT",   FAKE_FINGERPRINT);
            setField(Build.class, "MODEL",          FAKE_MODEL);
            setField(Build.class, "BRAND",          FAKE_BRAND);
            setField(Build.class, "DEVICE",         FAKE_DEVICE);
            setField(Build.class, "MANUFACTURER",   FAKE_MANUFACTURER);
            setField(Build.class, "TAGS",           FAKE_TAGS);
            setField(Build.class, "TYPE",           FAKE_TYPE);
            // Also spoof VERSION inner class
            setField(Build.VERSION.class, "RELEASE", "13");
            Slog.d(TAG, "Build fields spoofed");
        } catch (Throwable e) {
            Slog.w(TAG, "Build field spoof partial failure: " + e.getMessage());
        }
    }

    private static void setField(Class<?> cls, String name, String value) {
        try {
            Field f = cls.getDeclaredField(name);
            f.setAccessible(true);
            f.set(null, value);
        } catch (Throwable e) {
            Slog.w(TAG, "setField(" + name + ") failed: " + e.getMessage());
        }
    }

    // ── System.getProperty spoofing ───────────────────────────────────────────

    private void spoofSystemProperties() {
        try {
            System.setProperty("ro.debuggable",          "1");
            System.setProperty("ro.secure",              "0");
            System.setProperty("service.adb.root",       "1");
            System.setProperty("ro.build.tags",          FAKE_TAGS);
            System.setProperty("ro.build.type",          FAKE_TYPE);
            System.setProperty("ro.product.model",       FAKE_MODEL);
            System.setProperty("ro.product.brand",       FAKE_BRAND);
            System.setProperty("ro.product.device",      FAKE_DEVICE);
            System.setProperty("ro.product.manufacturer",FAKE_MANUFACTURER);
            System.setProperty("ro.build.fingerprint",   FAKE_FINGERPRINT);
            System.setProperty("ro.kernel.qemu",         "0");
            System.setProperty("ro.boot.qemu",           "0");
            Slog.d(TAG, "System properties spoofed");
        } catch (Throwable e) {
            Slog.w(TAG, "System property spoof failed: " + e.getMessage());
        }
    }
}
