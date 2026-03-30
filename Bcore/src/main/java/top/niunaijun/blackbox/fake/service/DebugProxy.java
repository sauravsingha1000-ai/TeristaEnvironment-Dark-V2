package top.niunaijun.blackbox.fake.service;

import android.os.Debug;
import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * DebugProxy — Spoofs android.os.Debug to prevent anti-debug detection.
 *
 * GameGuardian and root checkers call:
 *   - Debug.isDebuggerConnected() → must return false
 *   - Debug.waitingForDebugger()  → must return false
 *
 * This ensures the virtual app cannot detect it is running inside a
 * debuggable environment or virtual container via the Debug API.
 *
 * NOTE: This only affects code running inside the virtual space.
 * The real device's Debug state is completely unaffected.
 */
public class DebugProxy extends ClassInvocationStub {

    private static final String TAG = "DebugProxy";

    @Override
    protected Object getWho() {
        return Debug.class;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {}

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("isDebuggerConnected")
    public static class IsDebuggerConnected extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) {
            Slog.d(TAG, "isDebuggerConnected() -> false (spoofed)");
            return false;
        }
    }

    @ProxyMethod("waitingForDebugger")
    public static class WaitingForDebugger extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) {
            Slog.d(TAG, "waitingForDebugger() -> false (spoofed)");
            return false;
        }
    }

    @ProxyMethod("getNativeHeapSize")
    public static class GetNativeHeapSize extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) {
            // Return a plausible real-device value (64 MB)
            return 67108864L;
        }
    }
}
