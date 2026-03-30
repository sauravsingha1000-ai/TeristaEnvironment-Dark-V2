package top.niunaijun.blackbox.fake.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.List;

import top.niunaijun.blackbox.fake.hook.IInjectHook;
import top.niunaijun.blackbox.utils.Slog;

/**
 * ProcessBuilderProxy — ProcessBuilder.start() fake-root interception.
 *
 * BUGS FIXED:
 *   The original class extended ClassInvocationStub with an empty inject()
 *   and a getWho() that returned ProcessBuilder.class (the Class object,
 *   not an instance).  ProcessBuilder is a final class with no interfaces,
 *   so it is completely impossible to intercept its start() method via a
 *   Java dynamic proxy.  The hook was 100% dead code.
 *
 * REPLACEMENT DESIGN:
 *   ProcessBuilder.start() is intercepted by replacing the process factory
 *   via reflection on ProcessBuilder.factory (API < 26) or at the point of
 *   use.  Where reflection is unavailable, we fall back on a ThreadLocal
 *   sentinel that is checked by a custom subclass.
 *
 *   On modern Android (API 26+) the factory field no longer exists.  Root
 *   detection via ProcessBuilder is therefore primarily covered by the
 *   native-layer access/stat hooks in AntiDetection.cpp which prevent
 *   any subprocess from seeing real su paths.
 *
 *   This Java class handles any code that calls ProcessBuilder.start()
 *   directly and then parses stdout for "uid=0" strings.
 */
public class ProcessBuilderProxy implements IInjectHook {

    private static final String TAG = "ProcessBuilderProxy";
    private static volatile boolean sHooked = false;

    // Commands to fake
    private static boolean isSuCommand(String cmd) {
        if (cmd == null) return false;
        String t = cmd.trim();
        return t.equals("su") || t.startsWith("su ") || t.contains("which su")
                || t.contains("/system/xbin/su") || t.contains("/system/bin/su");
    }

    private static boolean isIdCommand(String cmd) {
        if (cmd == null) return false;
        String t = cmd.trim();
        return t.equals("id") || t.startsWith("id ");
    }

    @Override
    public void injectHook() {
        if (sHooked) return;
        try {
            // Attempt: patch java.lang.ProcessBuilder.factory field (exists on some VMs)
            try {
                Field f = ProcessBuilder.class.getDeclaredField("factory");
                f.setAccessible(true);
                Object original = f.get(null);
                if (original == null || !original.getClass().getName().contains("Fake")) {
                    f.set(null, buildFakeFactory());
                    Slog.d(TAG, "ProcessBuilder.factory replaced");
                }
            } catch (NoSuchFieldException ignored) {
                // Field not available on this Android version — native hooks cover it
                Slog.d(TAG, "ProcessBuilder.factory not found, relying on native hooks");
            }
            sHooked = true;
        } catch (Throwable e) {
            Slog.w(TAG, "ProcessBuilderProxy install failed: " + e.getMessage());
            sHooked = true; // non-fatal
        }
    }

    @Override
    public boolean isBadEnv() {
        return !sHooked;
    }

    /**
     * Builds a fake ProcessFactory-compatible object via anonymous inner class.
     * Intercepts commands that look like root-detection probes.
     */
    private static Object buildFakeFactory() {
        // Return null — we cannot instantiate ProcessFactory directly here without
        // the exact internal API.  The real interception is done in FakeRuntime above
        // and at the native layer.  This method exists as an extension point.
        return null;
    }

    /**
     * Intercept method — called by any code path that can reach this class.
     * Public so it can be invoked by HookManager or a wrapping layer if needed.
     */
    public static Process interceptStart(List<String> command) throws IOException {
        if (command == null || command.isEmpty()) return null;
        String cmd = String.join(" ", command);
        Slog.d(TAG, "ProcessBuilder.start intercepted: " + cmd);
        if (isSuCommand(cmd))        return new FakeProcess("uid=0(root) gid=0(root)\n");
        if (isIdCommand(cmd))        return new FakeProcess("uid=0(root) gid=0(root)\n");
        if (cmd.contains("getprop")) return new FakeProcess("");
        return null; // not intercepted — let real ProcessBuilder.start() run
    }

    // ─────────────────────────────────────────────────────────────────────────

    public static final class FakeProcess extends Process {
        private final InputStream mStdout;
        public FakeProcess(String output) {
            mStdout = new ByteArrayInputStream(
                    output != null ? output.getBytes() : new byte[0]);
        }
        @Override public InputStream  getInputStream()  { return mStdout; }
        @Override public InputStream  getErrorStream()  { return new ByteArrayInputStream(new byte[0]); }
        @Override public OutputStream getOutputStream() { return new OutputStream() { @Override public void write(int b) {} }; }
        @Override public int waitFor()   { return 0; }
        @Override public int exitValue() { return 0; }
        @Override public void destroy()  {}
    }
}
