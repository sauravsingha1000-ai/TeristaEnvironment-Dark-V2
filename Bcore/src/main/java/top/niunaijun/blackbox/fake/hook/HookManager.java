package top.niunaijun.blackbox.fake.hook;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.delegate.AppInstrumentation;

import top.niunaijun.blackbox.fake.service.HCallbackProxy;
import top.niunaijun.blackbox.fake.service.IAccessibilityManagerProxy;
import top.niunaijun.blackbox.fake.service.IAccountManagerProxy;
import top.niunaijun.blackbox.fake.service.IActivityClientProxy;
import top.niunaijun.blackbox.fake.service.IActivityManagerProxy;
import top.niunaijun.blackbox.fake.service.IActivityTaskManagerProxy;
import top.niunaijun.blackbox.fake.service.IAlarmManagerProxy;
import top.niunaijun.blackbox.fake.service.IAppOpsManagerProxy;
import top.niunaijun.blackbox.fake.service.IAppWidgetManagerProxy;
import top.niunaijun.blackbox.fake.service.IAttributionSourceProxy;
import top.niunaijun.blackbox.fake.service.IAutofillManagerProxy;
import top.niunaijun.blackbox.fake.service.ISensitiveContentProtectionManagerProxy;
import top.niunaijun.blackbox.fake.service.ISettingsSystemProxy;
import top.niunaijun.blackbox.fake.service.IConnectivityManagerProxy;
import top.niunaijun.blackbox.fake.service.ISystemSensorManagerProxy;
import top.niunaijun.blackbox.fake.service.IContentProviderProxy;
import top.niunaijun.blackbox.fake.service.IXiaomiAttributionSourceProxy;
import top.niunaijun.blackbox.fake.service.IXiaomiSettingsProxy;
import top.niunaijun.blackbox.fake.service.IXiaomiMiuiServicesProxy;
import top.niunaijun.blackbox.fake.service.IDnsResolverProxy;
import top.niunaijun.blackbox.fake.service.IContextHubServiceProxy;
import top.niunaijun.blackbox.fake.service.IDeviceIdentifiersPolicyProxy;
import top.niunaijun.blackbox.fake.service.IDevicePolicyManagerProxy;
import top.niunaijun.blackbox.fake.service.IDisplayManagerProxy;
import top.niunaijun.blackbox.fake.service.IFingerprintManagerProxy;
import top.niunaijun.blackbox.fake.service.IGraphicsStatsProxy;
import top.niunaijun.blackbox.fake.service.IJobServiceProxy;
import top.niunaijun.blackbox.fake.service.ILauncherAppsProxy;
import top.niunaijun.blackbox.fake.service.ILocationManagerProxy;
import top.niunaijun.blackbox.fake.service.IMediaRouterServiceProxy;
import top.niunaijun.blackbox.fake.service.IMediaSessionManagerProxy;
import top.niunaijun.blackbox.fake.service.IAudioServiceProxy;
import top.niunaijun.blackbox.fake.service.ISensorPrivacyManagerProxy;
import top.niunaijun.blackbox.fake.service.ContentResolverProxy;
import top.niunaijun.blackbox.fake.service.IWebViewUpdateServiceProxy;
import top.niunaijun.blackbox.fake.service.IMiuiSecurityManagerProxy;
import top.niunaijun.blackbox.fake.service.SystemLibraryProxy;
import top.niunaijun.blackbox.fake.service.ReLinkerProxy;
import top.niunaijun.blackbox.fake.service.WebViewProxy;
import top.niunaijun.blackbox.fake.service.WebViewFactoryProxy;
import top.niunaijun.blackbox.fake.service.MediaRecorderProxy;
import top.niunaijun.blackbox.fake.service.AudioRecordProxy;
import top.niunaijun.blackbox.fake.service.MediaRecorderClassProxy;
import top.niunaijun.blackbox.fake.service.SQLiteDatabaseProxy;
import top.niunaijun.blackbox.fake.service.ClassLoaderProxy;
import top.niunaijun.blackbox.fake.service.GmsProxy;
import top.niunaijun.blackbox.fake.service.LevelDbProxy;
import top.niunaijun.blackbox.fake.service.DeviceIdProxy;
import top.niunaijun.blackbox.fake.service.GoogleAccountManagerProxy;
import top.niunaijun.blackbox.fake.service.AuthenticationProxy;
import top.niunaijun.blackbox.fake.service.AndroidIdProxy;
import top.niunaijun.blackbox.fake.service.AudioPermissionProxy;

// ── Fake-root environment hooks (all now implement IInjectHook directly) ──
import top.niunaijun.blackbox.fake.service.RuntimeProxy;
import top.niunaijun.blackbox.fake.service.ProcessBuilderProxy;
import top.niunaijun.blackbox.fake.service.SystemPropertiesProxy;
import top.niunaijun.blackbox.fake.service.BuildProxy;
// REMOVED: ProcessProxy — android.os.Process is a static utility class, cannot be proxied
//          via ClassInvocationStub. OsStub.getuid() already returns the correct virtual UID.
// REMOVED: OsProxy — getWho() returned Class.forName("libcore.io.Os") which is a Class
//          object, not the Os instance. OsStub correctly replaces Libcore.os instead.
import top.niunaijun.blackbox.fake.service.FileSystemProxy;
import top.niunaijun.blackbox.fake.service.FileInputStreamProxy;
import top.niunaijun.blackbox.fake.service.DebugProxy;

import top.niunaijun.blackbox.fake.service.INetworkManagementServiceProxy;
import top.niunaijun.blackbox.fake.service.INotificationManagerProxy;
import top.niunaijun.blackbox.fake.service.IPackageManagerProxy;
import top.niunaijun.blackbox.fake.service.IPermissionManagerProxy;
import top.niunaijun.blackbox.fake.service.IPersistentDataBlockServiceProxy;
import top.niunaijun.blackbox.fake.service.IPhoneSubInfoProxy;
import top.niunaijun.blackbox.fake.service.IPowerManagerProxy;
import top.niunaijun.blackbox.fake.service.ApkAssetsProxy;
import top.niunaijun.blackbox.fake.service.ResourcesManagerProxy;
import top.niunaijun.blackbox.fake.service.IShortcutManagerProxy;
import top.niunaijun.blackbox.fake.service.IStorageManagerProxy;
import top.niunaijun.blackbox.fake.service.IStorageStatsManagerProxy;
import top.niunaijun.blackbox.fake.service.ISystemUpdateProxy;
import top.niunaijun.blackbox.fake.service.ITelephonyManagerProxy;
import top.niunaijun.blackbox.fake.service.ITelephonyRegistryProxy;
import top.niunaijun.blackbox.fake.service.IUserManagerProxy;
import top.niunaijun.blackbox.fake.service.IVibratorServiceProxy;
import top.niunaijun.blackbox.fake.service.IVpnManagerProxy;
import top.niunaijun.blackbox.fake.service.IWifiManagerProxy;
import top.niunaijun.blackbox.fake.service.IWifiScannerProxy;
import top.niunaijun.blackbox.fake.service.IWindowManagerProxy;
import top.niunaijun.blackbox.fake.service.context.ContentServiceStub;
import top.niunaijun.blackbox.fake.service.context.RestrictionsManagerStub;
import top.niunaijun.blackbox.fake.service.libcore.OsStub;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.BuildCompat;
import top.niunaijun.blackbox.fake.service.ISettingsProviderProxy;
import top.niunaijun.blackbox.fake.service.FeatureFlagUtilsProxy;
import top.niunaijun.blackbox.fake.service.WorkManagerProxy;

/**
 * HookManager — registers and installs all IInjectHook instances.
 *
 * BUGS FIXED:
 *   1. OsProxy removed — it incorrectly passed android.os.Process.class as
 *      getWho(), producing a proxy of java.lang.Class rather than the libcore
 *      Os instance. OsStub already handles this correctly via BRLibcore.
 *
 *   2. ProcessProxy removed — android.os.Process is a static utility class
 *      with no instance to proxy. The correct UID-spoofing path is OsStub.
 *
 *   3. FileSystemProxy, FileInputStreamProxy, RuntimeProxy, ProcessBuilderProxy
 *      now implement IInjectHook directly (they no longer extend the broken
 *      ClassInvocationStub pattern for non-interface targets).
 *
 *   4. addInjector() now uses the canonical class name as key (not Class<?>)
 *      so that multiple instances of different concrete types don't collide.
 */
public class HookManager {
    public static final String TAG = "HookManager";

    private static final HookManager sHookManager = new HookManager();

    // Keyed by canonical class name to avoid collisions between subtypes
    private final Map<String, IInjectHook> mInjectors = new HashMap<>();

    public static HookManager get() {
        return sHookManager;
    }

    public void init() {
        if (BlackBoxCore.get().isBlackProcess() || BlackBoxCore.get().isServerProcess()) {

            // ── Core binder hooks ────────────────────────────────────────────
            addInjector(new IDisplayManagerProxy());
            addInjector(new OsStub());                  // libcore.io.Os uid/stat spoof
            addInjector(new IActivityManagerProxy());
            addInjector(new IPackageManagerProxy());
            addInjector(new ITelephonyManagerProxy());
            addInjector(new HCallbackProxy());
            addInjector(new IAppOpsManagerProxy());
            addInjector(new INotificationManagerProxy());
            addInjector(new IAlarmManagerProxy());
            addInjector(new IAppWidgetManagerProxy());
            addInjector(new ContentServiceStub());
            addInjector(new IWindowManagerProxy());
            addInjector(new IUserManagerProxy());
            addInjector(new RestrictionsManagerStub());
            addInjector(new IMediaSessionManagerProxy());
            addInjector(new IAudioServiceProxy());
            addInjector(new ISensorPrivacyManagerProxy());
            addInjector(new ContentResolverProxy());
            addInjector(new IWebViewUpdateServiceProxy());
            addInjector(new SystemLibraryProxy());
            addInjector(new ReLinkerProxy());
            addInjector(new WebViewProxy());
            addInjector(new WebViewFactoryProxy());
            addInjector(new WorkManagerProxy());
            addInjector(new MediaRecorderProxy());
            addInjector(new AudioRecordProxy());
            addInjector(new IMiuiSecurityManagerProxy());
            addInjector(new ISettingsProviderProxy());
            addInjector(new FeatureFlagUtilsProxy());
            addInjector(new MediaRecorderClassProxy());
            addInjector(new SQLiteDatabaseProxy());
            addInjector(new ClassLoaderProxy());

            // ── Fake-root environment (Java layer) ───────────────────────────
            // Order matters: FileSystemProxy/FileInputStreamProxy apply Build spoofs first,
            // then SystemPropertiesProxy and BuildProxy extend/overwrite specific values,
            // then RuntimeProxy installs the exec interceptor.
            addInjector(new FileSystemProxy());        // Build fields + System.setProperty
            addInjector(new FileInputStreamProxy());   // SystemProperties.get() spoof
            addInjector(new SystemPropertiesProxy());  // android.os.SystemProperties proxy
            addInjector(new BuildProxy());             // Build.* field direct patch
            addInjector(new RuntimeProxy());           // Runtime.exec() interception
            addInjector(new ProcessBuilderProxy());    // ProcessBuilder.start() interception
            addInjector(new DebugProxy());             // Debug.isDebuggerConnected() → false (GG bypass)

            // ── Identity / GMS ───────────────────────────────────────────────
            addInjector(new GmsProxy());
            addInjector(new LevelDbProxy());
            addInjector(new DeviceIdProxy());
            addInjector(new GoogleAccountManagerProxy());
            addInjector(new AuthenticationProxy());
            addInjector(new AndroidIdProxy());
            addInjector(new AudioPermissionProxy());

            // ── Service hooks ────────────────────────────────────────────────
            addInjector(new ILocationManagerProxy());
            addInjector(new IStorageManagerProxy());
            addInjector(new ILauncherAppsProxy());
            addInjector(new IJobServiceProxy());
            addInjector(new IAccessibilityManagerProxy());
            addInjector(new ITelephonyRegistryProxy());
            addInjector(new IDevicePolicyManagerProxy());
            addInjector(new IAccountManagerProxy());
            addInjector(new IConnectivityManagerProxy());
            addInjector(new IDnsResolverProxy());
            addInjector(new IAttributionSourceProxy());
            addInjector(new IContentProviderProxy());
            addInjector(new ISettingsSystemProxy());
            addInjector(new ISystemSensorManagerProxy());

            addInjector(new IXiaomiAttributionSourceProxy());
            addInjector(new IXiaomiSettingsProxy());
            addInjector(new IXiaomiMiuiServicesProxy());
            addInjector(new IPhoneSubInfoProxy());
            addInjector(new IMediaRouterServiceProxy());
            addInjector(new IPowerManagerProxy());
            addInjector(new IContextHubServiceProxy());
            addInjector(new IVibratorServiceProxy());
            addInjector(new IPersistentDataBlockServiceProxy());
            addInjector(AppInstrumentation.get());

            addInjector(new IWifiManagerProxy());
            addInjector(new IWifiScannerProxy());
            addInjector(new ApkAssetsProxy());
            addInjector(new ResourcesManagerProxy());

            // ── Version-gated hooks ─────────────────────────────────────────
            if (BuildCompat.isS()) {
                addInjector(new IActivityClientProxy(null));
                addInjector(new IVpnManagerProxy());
                addInjector(new ISensitiveContentProtectionManagerProxy());
            }
            if (BuildCompat.isR()) {
                addInjector(new IPermissionManagerProxy());
            }
            if (BuildCompat.isQ()) {
                addInjector(new IActivityTaskManagerProxy());
            }
            if (BuildCompat.isPie()) {
                addInjector(new ISystemUpdateProxy());
            }
            if (BuildCompat.isOreo()) {
                addInjector(new IAutofillManagerProxy());
                addInjector(new IDeviceIdentifiersPolicyProxy());
                addInjector(new IStorageStatsManagerProxy());
            }
            if (BuildCompat.isN_MR1()) {
                addInjector(new IShortcutManagerProxy());
            }
            if (BuildCompat.isN()) {
                addInjector(new INetworkManagementServiceProxy());
            }
            if (BuildCompat.isM()) {
                addInjector(new IFingerprintManagerProxy());
                addInjector(new IGraphicsStatsProxy());
            }
            if (BuildCompat.isL()) {
                addInjector(new IJobServiceProxy());
            }
        }
        injectAll();
    }

    public void checkEnv(Class<?> clazz) {
        IInjectHook hook = mInjectors.get(clazz.getCanonicalName());
        if (hook != null && hook.isBadEnv()) {
            Log.d(TAG, "checkEnv: " + clazz.getSimpleName() + " is bad env, re-injecting");
            try { hook.injectHook(); } catch (Exception e) { Slog.e(TAG, "Re-inject failed", e); }
        }
    }

    public void checkAll() {
        for (Map.Entry<String, IInjectHook> entry : mInjectors.entrySet()) {
            IInjectHook hook = entry.getValue();
            if (hook != null && hook.isBadEnv()) {
                Log.d(TAG, "checkEnv: " + entry.getKey() + " is bad env, re-injecting");
                try { hook.injectHook(); }
                catch (Exception e) { Slog.e(TAG, "Re-inject failed for " + entry.getKey(), e); }
            }
        }
    }

    void addInjector(IInjectHook injectHook) {
        mInjectors.put(injectHook.getClass().getCanonicalName(), injectHook);
    }

    void injectAll() {
        for (Map.Entry<String, IInjectHook> entry : mInjectors.entrySet()) {
            try {
                Slog.d(TAG, "hook: " + entry.getKey());
                entry.getValue().injectHook();
            } catch (Exception e) {
                handleHookError(entry.getKey(), entry.getValue(), e);
            }
        }
    }

    private void handleHookError(String name, IInjectHook hook, Exception e) {
        Slog.e(TAG, "Hook failed: " + name + " — " + e.getMessage(), e);
        // Attempt recovery only for hooks that are critical to virtual process isolation
        if (name.contains("ActivityManager") || name.contains("PackageManager")
                || name.contains("WebView")   || name.contains("ContentProvider")) {
            Slog.w(TAG, "Critical hook — attempting recovery: " + name);
            try {
                if (hook.isBadEnv()) hook.injectHook();
            } catch (Exception re) {
                Slog.e(TAG, "Recovery failed: " + name, re);
            }
        }
    }

    public boolean areCriticalHooksInstalled() {
        String[] critical = {
            IActivityManagerProxy.class.getCanonicalName(),
            IPackageManagerProxy.class.getCanonicalName(),
            WebViewProxy.class.getCanonicalName(),
            IContentProviderProxy.class.getCanonicalName()
        };
        for (String name : critical) {
            if (!mInjectors.containsKey(name)) {
                Slog.w(TAG, "Critical hook missing: " + name);
                return false;
            }
        }
        return true;
    }

    public void reinitializeHooks() {
        Slog.d(TAG, "Reinitializing all hooks");
        mInjectors.clear();
        init();
        Slog.d(TAG, "Hook reinitialization complete");
    }
}
