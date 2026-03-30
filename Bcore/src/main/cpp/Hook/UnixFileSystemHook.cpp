/**
 * UnixFileSystemHook.cpp
 *
 * FIXES APPLIED:
 *   1. getBooleanAttributes0 was defined as a HOOK_JNI function but was NEVER
 *      registered in init() — File.exists() path-redirection was dead code.
 *      Fixed: added the missing HookJniFun call for getBooleanAttributes0.
 *   2. getBooleanAttributes0 now returns BA_EXISTS (0x01) for su paths so that
 *      java.io.File.exists("/system/xbin/su") returns true inside virtual space.
 */

#include <IO.h>
#include "UnixFileSystemHook.h"
#import "JniHook/JniHook.h"
#include "BaseHook.h"
#include <cstring>
#include <android/log.h>

#define LOG_TAG "UnixFileSystemHook"
#define ALOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

/* BA_EXISTS bit used by java.io.UnixFileSystem.getBooleanAttributes0() */
static const jint BA_EXISTS = 0x01;

static const char* kFakeExistJavaPaths[] = {
    "/system/xbin/su",
    "/system/bin/su",
    "/sbin/su",
    nullptr
};

static bool is_su_path(const char* path) {
    if (!path) return false;
    for (int i = 0; kFakeExistJavaPaths[i]; ++i)
        if (strcmp(path, kFakeExistJavaPaths[i]) == 0) return true;
    return false;
}

HOOK_JNI(jstring, canonicalize0, JNIEnv *env, jobject obj, jstring path) {
    jstring redirect = IO::redirectPath(env, path);
    return orig_canonicalize0(env, obj, redirect);
}

/**
 * getBooleanAttributes0 — BUG FIX
 *
 * This hook was defined but never registered in init().
 * Every File.exists() / File.isFile() call passed through unmodified,
 * meaning path redirection had no effect for existence checks.
 *
 * Additionally: fake su paths now return BA_EXISTS so that guest apps
 * perceive /system/xbin/su as existing (virtual root simulation).
 */
HOOK_JNI(jint, getBooleanAttributes0, JNIEnv *env, jobject obj, jstring abspath) {
    if (abspath) {
        const char* path = env->GetStringUTFChars(abspath, nullptr);
        if (path) {
            bool suPath = is_su_path(path);
            env->ReleaseStringUTFChars(abspath, path);
            if (suPath) {
                ALOGD("[FakeRoot] getBooleanAttributes0 -> BA_EXISTS for su path");
                return BA_EXISTS;
            }
        }
    }
    jstring redirect = IO::redirectPath(env, abspath);
    return orig_getBooleanAttributes0(env, obj, redirect);
}

HOOK_JNI(jlong, getLastModifiedTime0, JNIEnv *env, jobject obj, jobject path) {
    jobject redirect = IO::redirectPath(env, path);
    return orig_getLastModifiedTime0(env, obj, redirect);
}

HOOK_JNI(jboolean, setPermission0, JNIEnv *env, jobject obj, jobject file, jint access,
         jboolean enable, jboolean owneronly) {
    jobject redirect = IO::redirectPath(env, file);
    return orig_setPermission0(env, obj, redirect, access, enable, owneronly);
}

HOOK_JNI(jboolean, createFileExclusively0, JNIEnv *env, jobject obj, jstring path) {
    jstring redirect = IO::redirectPath(env, path);
    return orig_createFileExclusively0(env, obj, redirect);
}

HOOK_JNI(jobjectArray, list0, JNIEnv *env, jobject obj, jobject file) {
    jobject redirect = IO::redirectPath(env, file);
    return orig_list0(env, obj, redirect);
}

HOOK_JNI(jboolean, createDirectory0, JNIEnv *env, jobject obj, jobject path) {
    jobject redirect = IO::redirectPath(env, path);
    return orig_createDirectory0(env, obj, redirect);
}

HOOK_JNI(jboolean, setLastModifiedTime0, JNIEnv *env, jobject obj, jobject file, jobject time) {
    jobject redirect = IO::redirectPath(env, file);
    return orig_setLastModifiedTime0(env, obj, redirect, time);
}

HOOK_JNI(jboolean, setReadOnly0, JNIEnv *env, jobject obj, jobject file) {
    jobject redirect = IO::redirectPath(env, file);
    return orig_setReadOnly0(env, obj, redirect);
}

HOOK_JNI(jboolean, getSpace0, JNIEnv *env, jobject obj, jobject file, jint t) {
    jobject redirect = IO::redirectPath(env, file);
    return orig_getSpace0(env, obj, redirect, t);
}

void UnixFileSystemHook::init(JNIEnv *env) {
    const char* cls = "java/io/UnixFileSystem";

#define SAFE_HOOK(method, sig, new_fn, orig_pp)                                    \
    try {                                                                            \
        JniHook::HookJniFun(env, cls, method, sig,                                 \
                             (void*)(new_fn), (void**)(orig_pp), false);            \
        ALOGD("Hooked: " method);                                                  \
    } catch (...) {                                                                 \
        ALOGD("Failed to hook: " method " (non-fatal)");                           \
    }

    SAFE_HOOK("canonicalize0",
              "(Ljava/lang/String;)Ljava/lang/String;",
              new_canonicalize0, &orig_canonicalize0)

    /* BUG FIX: getBooleanAttributes0 was never registered */
    SAFE_HOOK("getBooleanAttributes0",
              "(Ljava/lang/String;)I",
              new_getBooleanAttributes0, &orig_getBooleanAttributes0)

    SAFE_HOOK("getLastModifiedTime0",
              "(Ljava/io/File;)J",
              new_getLastModifiedTime0, &orig_getLastModifiedTime0)

    SAFE_HOOK("setPermission0",
              "(Ljava/io/File;IZZ)Z",
              new_setPermission0, &orig_setPermission0)

    SAFE_HOOK("createFileExclusively0",
              "(Ljava/lang/String;)Z",
              new_createFileExclusively0, &orig_createFileExclusively0)

    SAFE_HOOK("list0",
              "(Ljava/io/File;)[Ljava/lang/String;",
              new_list0, &orig_list0)

    SAFE_HOOK("createDirectory0",
              "(Ljava/io/File;)Z",
              new_createDirectory0, &orig_createDirectory0)

    SAFE_HOOK("setLastModifiedTime0",
              "(Ljava/io/File;J)Z",
              new_setLastModifiedTime0, &orig_setLastModifiedTime0)

    SAFE_HOOK("setReadOnly0",
              "(Ljava/io/File;)Z",
              new_setReadOnly0, &orig_setReadOnly0)

    SAFE_HOOK("getSpace0",
              "(Ljava/io/File;I)J",
              new_getSpace0, &orig_getSpace0)

#undef SAFE_HOOK
}
