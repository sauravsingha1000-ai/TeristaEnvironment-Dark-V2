package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;

public class OsProxy extends ClassInvocationStub {

    @Override
    protected Object getWho() {
        try {
            return Class.forName("libcore.io.Os");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {}

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getuid")
    public static class GetUid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) {
            return 0; // ✅ root
        }
    }
}
