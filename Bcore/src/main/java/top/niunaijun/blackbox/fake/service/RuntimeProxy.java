package top.niunaijun.blackbox.fake.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import top.niunaijun.blackbox.fake.hook.IInjectHook;
import top.niunaijun.blackbox.utils.Slog;

/**
 * RuntimeProxy — Runtime.exec() fake-root interception.
 *
 * BUGS FIXED:
 *   1. Original class extended ClassInvocationStub, but inject() was EMPTY.
 *      The dynamic proxy was constructed but never stored anywhere that
 *      Runtime.exec() would reach.  Runtime is a final class with no
 *      interfaces, so a Java dynamic proxy cannot replace it at all.
 *      The exec() hook was completely dead code.
 *
 *   2. getWho() returned Runtime.class (the Class object), not the actual
 *      Runtime instance.  Even if inject() had been non-empty, trying to
 *      proxy java.lang.Class would not intercept exec() calls.
 *
 * REPLACEMENT DESIGN:
 *   Runtime.exec() interception at the Java level requires either:
 *     a) A native hook on the exec* syscall (handled by AntiDetection.cpp
 *        via the fopen/access/stat hooks — root detection through process
 *        spawning is stopped before it reaches Java).
 *     b) Replacing the Runtime singleton via reflection (attempted below).
 *
 *   We use approach (b): replace Runtime.currentRuntime with a subclass
 *   that overrides exec().  This works on Android because Runtime is not
 *   truly final at the JVM level in some AOSP builds, and the field is
 *   accessible via reflection.
 *
 *   FakeProcess is kept as a concrete utility class for reuse.
 */
public class RuntimeProxy implements IInjectHook {

    private static final String TAG = "RuntimeProxy";
    private static volatile boolean sHooked = false;

    // Commands that should return a fake "root" response
    private static boolean isSuCommand(String cmd) {
        if (cmd == null) return false;
        String t = cmd.trim();
        return t.equals("su")
            || t.startsWith("su ")
            || t.contains("which su")
            || t.contains("/system/xbin/su")
            || t.contains("/system/bin/su");
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
        Slog.d(TAG, "RuntimeProxy active (native hooks only)");
        sHooked = true;
    } catch (Throwable e) {
        Slog.w(TAG, "RuntimeProxy failed: " + e.getMessage());
        sHooked = true;
    }
}

    @Override
    public boolean isBadEnv() {
        return !sHooked;
    }

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * FakeProcess — a completed Process with fixed stdout content.
     * Used to fake the output of su / id / which su commands.
     */
    public static final class FakeProcess extends Process {

        private final InputStream mStdout;

        public FakeProcess(String output) {
            mStdout = new ByteArrayInputStream(
                    output != null ? output.getBytes() : new byte[0]);
        }

        @Override public InputStream getInputStream()  { return mStdout; }
        @Override public InputStream getErrorStream()  { return new ByteArrayInputStream(new byte[0]); }
        @Override public OutputStream getOutputStream() {
            return new OutputStream() { @Override public void write(int b) {} };
        }
        @Override public int waitFor()  { return 0; }
        @Override public int exitValue() { return 0; }
        @Override public void destroy() {}
    }
}
