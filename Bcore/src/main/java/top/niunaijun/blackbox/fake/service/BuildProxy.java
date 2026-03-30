package top.niunaijun.blackbox.fake.service;

import android.os.Build;
import java.lang.reflect.Field;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;

public class BuildProxy extends ClassInvocationStub {

    @Override
    protected Object getWho() {
        return Build.class;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        try {
            set("TAGS", "release-keys");
            set("FINGERPRINT", "google/redfin/redfin:13/TP1A.220624.014/1234567:user/release-keys");
            set("MODEL", "Pixel 5");
            set("BRAND", "google");
            set("DEVICE", "redfin");
            set("MANUFACTURER", "Google");
        } catch (Throwable ignored) {}
    }

    private void set(String name, String value) throws Exception {
        Field f = Build.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
