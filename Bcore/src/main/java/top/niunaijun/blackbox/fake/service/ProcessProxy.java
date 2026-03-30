package top.niunaijun.blackbox.fake.service;

import android.os.Process;
import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;

public class ProcessProxy extends ClassInvocationStub {

    @Override
    protected Object getWho() {
        return Process.class;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {}

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("myUid")
    public static class MyUid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) {
            return 0; // ✅ root uid
        }
    }
}
