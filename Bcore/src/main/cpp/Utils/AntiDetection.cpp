/**
 * AntiDetection.cpp — Fake Root Simulation + Anti-Detection hooks
 *
 * FIXED:
 *   1. install_file_hooks() now actually calls DobbyHook (was empty before).
 *   2. /proc/self/status spoofed with uid=0 and TracerPid=0.
 *   3. /proc/net/tcp spoofed to hide daemon ports (GameGuardian bypass).
 *   4. /proc/<pid>/maps filtered to remove blackbox/virtual lib references.
 *   5. fopen + open hooks cover all major root/daemon detection paths.
 *   6. Real device not touched — all hooks apply only inside the guest process.
 */

#include <android/log.h>
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <dirent.h>
#include <stdarg.h>
#include <stdlib.h>
#include "Dobby/dobby.h"
#include "xdl.h"

#define LOG_TAG "AntiDetection"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/* ── Paths that MUST appear to exist (fake-root simulation) ── */
static const char* kFakeExistPaths[] = {
    "/system/xbin/su",
    "/system/bin/su",
    "/sbin/su",
    nullptr
};

/* ── Paths that MUST be hidden from virtual apps ── */
static const char* kBlockedPaths[] = {
    "/system/app/Superuser.apk",
    "/system/app/SuperSU.apk",
    "/system/etc/init.d/99SuperSUDaemon",
    "/system/xbin/daemonsu",
    "/system/xbin/sugote",
    "/system/bin/sugote-mksh",
    "/system/xbin/sugote-mksh",
    "/data/local/xbin/su",
    "/data/local/bin/su",
    "/data/local/tmp/su",
    "/system/bin/magisk",
    "/system/xbin/magisk",
    "/sbin/magisk",
    "/data/adb/magisk",
    "/data/virtual",
    "/data/data/com.benny.openlauncher",
    "/data/data/io.va.exposed",
    "/data/data/com.lody.virtual",
    "/data/data/com.excelliance.dualaid",
    "/data/data/com.lbe.parallel",
    "/data/data/com.dual.dualspace",
    "/blackbox",
    "/virtual",
    "/dev/vboxguest",
    "/dev/vboxuser",
    "/dev/qemu_pipe",
    "/dev/goldfish_pipe",
    "/dev/socket/qemud",
    "/dev/socket/baseband_genyd",
    "/dev/socket/genyd",
    "/system/lib/libc_malloc_debug_qemu.so",
    "/sys/qemu_trace",
    "/system/bin/qemu-props",
    "/system/bin/nox-prop",
    "/sys/module/goldfish_audio",
    "/sys/module/goldfish_sync",
    "/dev/goldfish_events",
    "/system/lib/libdroid4x.so",
    "/system/lib/libnoxspeedup.so",
    "/system/lib/libmemu.so",
    "/system/xposed.prop",
    "/system/framework/XposedBridge.jar",
    "/data/data/de.robv.android.xposed.installer",
    "/data/data/org.meowcat.edxposed.manager",
    "/data/data/top.canyie.dreamland.manager",
    nullptr
};

/* ── Fake /proc/self/status — uid=0 (root), TracerPid=0 (no debugger) ── */
static const char kFakeProcStatus[] =
    "Name:\tapp_process64\n"
    "State:\tR (running)\n"
    "Tgid:\t1000\n"
    "Pid:\t1000\n"
    "PPid:\t1\n"
    "TracerPid:\t0\n"
    "Uid:\t0\t0\t0\t0\n"
    "Gid:\t0\t0\t0\t0\n"
    "FDSize:\t256\n"
    "Groups:\t0 1004 1007 1011 1015 1028\n"
    "VmPeak:\t 2097152 kB\n"
    "VmSize:\t 2097152 kB\n"
    "VmRSS:\t   65536 kB\n"
    "Threads:\t16\n";

/* ── Fake /proc/self/cgroup — clean (no sandbox cgroup markers) ── */
static const char kFakeProcCgroup[] =
    "12:memory:/\n"
    "11:freezer:/\n"
    "10:cpuset:/\n"
    "0::/\n";

/* ── Fake /proc/net/tcp — clean (no unusual open ports from daemon) ──
 * GameGuardian scans this for the virtual env daemon's listening port.
 * We return a minimal table with only a standard loopback entry.
 */
static const char kFakeProcNetTcp[] =
    "  sl  local_address rem_address   st tx_queue rx_queue "
    "tr tm->when retrnsmt   uid  timeout inode\n"
    "   0: 0100007F:13AD 00000000:0000 0A 00000000:00000000 "
    "00:00000000 00000000  1000        0 12345 1 0000000000000000 100 0 0 10 0\n";

/* ── Fake /proc/net/tcp6 — same logic ── */
static const char kFakeProcNetTcp6[] =
    "  sl  local_address                         remote_address"
    "                        st tx_queue rx_queue tr tm->when retrnsmt"
    "   uid  timeout inode\n";

/* ── Helpers ── */
static bool starts_with(const char* s, const char* prefix) {
    return s && prefix && strncmp(s, prefix, strlen(prefix)) == 0;
}
static bool is_fake_exist(const char* p) {
    if (!p) return false;
    for (int i = 0; kFakeExistPaths[i]; ++i)
        if (strcmp(p, kFakeExistPaths[i]) == 0) return true;
    return false;
}
static bool is_blocked(const char* p) {
    if (!p) return false;
    for (int i = 0; kBlockedPaths[i]; ++i)
        if (strstr(p, kBlockedPaths[i])) return true;
    return false;
}
static bool is_proc_status(const char* p) {
    return p && (strstr(p, "/proc/self/status") || strstr(p, "/proc/thread-self/status"));
}
static bool is_proc_cgroup(const char* p) {
    return p && (strstr(p, "/proc/self/cgroup") || strstr(p, "/proc/thread-self/cgroup"));
}
static bool is_proc_net_tcp(const char* p) {
    if (!p) return false;
    return (strcmp(p, "/proc/net/tcp") == 0 || strcmp(p, "/proc/net/tcp6") == 0);
}
/* /proc/<pid>/maps — filter out lines containing suspicious library names */
static bool is_proc_maps(const char* p) {
    if (!p) return false;
    return strstr(p, "/proc/self/maps") || strstr(p, "/proc/thread-self/maps") ||
           (strstr(p, "/proc/") && strstr(p, "/maps") && !strstr(p, "/smaps"));
}

/* ── Original function pointers ── */
static int     (*orig_access)  (const char*, int)          = nullptr;
static int     (*orig_stat)    (const char*, struct stat*)  = nullptr;
static int     (*orig_lstat)   (const char*, struct stat*)  = nullptr;
static FILE*   (*orig_fopen)   (const char*, const char*)  = nullptr;
static int     (*orig_open)    (const char*, int, ...)      = nullptr;
static ssize_t (*orig_readlink)(const char*, char*, size_t) = nullptr;
static DIR*    (*orig_opendir) (const char*)                = nullptr;

/* ── Hook implementations ── */

static int my_access(const char* path, int mode) {
    if (path) {
        if (is_fake_exist(path)) { LOGD("[FakeRoot] access(%s)=0", path); return 0; }
        if (is_blocked(path))    { errno = ENOENT; return -1; }
    }
    return orig_access ? orig_access(path, mode) : -1;
}

static int my_stat(const char* path, struct stat* buf) {
    if (path) {
        if (is_fake_exist(path)) {
            LOGD("[FakeRoot] stat(%s) fake", path);
            if (buf) {
                memset(buf, 0, sizeof(*buf));
                buf->st_mode  = S_IFREG | 0755;
                buf->st_size  = 1024;
                buf->st_uid   = 0;
                buf->st_gid   = 0;
                buf->st_nlink = 1;
            }
            return 0;
        }
        if (is_blocked(path)) { errno = ENOENT; return -1; }
    }
    return orig_stat ? orig_stat(path, buf) : -1;
}

static int my_lstat(const char* path, struct stat* buf) {
    if (path) {
        if (is_fake_exist(path)) {
            LOGD("[FakeRoot] lstat(%s) fake", path);
            if (buf) {
                memset(buf, 0, sizeof(*buf));
                buf->st_mode  = S_IFREG | 0755;
                buf->st_size  = 1024;
                buf->st_uid   = 0;
                buf->st_gid   = 0;
                buf->st_nlink = 1;
            }
            return 0;
        }
        if (is_blocked(path)) { errno = ENOENT; return -1; }
    }
    return orig_lstat ? orig_lstat(path, buf) : -1;
}

static FILE* make_fake_file(const char* content, size_t len) {
    FILE* f = tmpfile();
    if (f && content) { fwrite(content, 1, len, f); rewind(f); }
    return f;
}

/* ── Maps filter: remove lines with suspicious lib names ── */
static FILE* make_filtered_maps(FILE* real) {
    if (!real) return nullptr;
    /* Suspicious substrings that indicate sandbox presence */
    static const char* kSuspicious[] = {
        "blackbox", "newblackbox", "virtual", "VirtualXposed",
        "bcore", "Bcore", "terista", "niunaijun",
        nullptr
    };
    char buf[1024];
    char* filtered_content = (char*)malloc(256 * 1024); /* 256 KB buffer */
    if (!filtered_content) return real;
    size_t offset = 0;
    while (fgets(buf, sizeof(buf), real)) {
        bool suspicious = false;
        for (int i = 0; kSuspicious[i]; i++) {
            if (strstr(buf, kSuspicious[i])) { suspicious = true; break; }
        }
        if (!suspicious) {
            size_t len = strlen(buf);
            if (offset + len < 256 * 1024 - 1) {
                memcpy(filtered_content + offset, buf, len);
                offset += len;
            }
        }
    }
    fclose(real);
    filtered_content[offset] = '\0';
    FILE* fake = tmpfile();
    if (fake) { fwrite(filtered_content, 1, offset, fake); rewind(fake); }
    free(filtered_content);
    return fake ? fake : nullptr;
}

/* ── fopen hook ── */
static FILE* my_fopen(const char* path, const char* mode) {
    if (path) {
        if (is_proc_status(path)) {
            LOGD("[FakeRoot] fopen(%s) -> uid=0 status", path);
            return make_fake_file(kFakeProcStatus, sizeof(kFakeProcStatus) - 1);
        }
        if (is_proc_cgroup(path)) {
            LOGD("[FakeRoot] fopen(%s) -> fake cgroup", path);
            return make_fake_file(kFakeProcCgroup, sizeof(kFakeProcCgroup) - 1);
        }
        if (is_proc_net_tcp(path)) {
            LOGD("[GGBypass] fopen(%s) -> clean tcp table", path);
            const char* content = (strstr(path, "tcp6"))
                ? kFakeProcNetTcp6 : kFakeProcNetTcp;
            return make_fake_file(content, strlen(content));
        }
        if (is_proc_maps(path)) {
            LOGD("[GGBypass] fopen(%s) -> filtered maps", path);
            FILE* real = orig_fopen ? orig_fopen(path, mode) : nullptr;
            return make_filtered_maps(real);
        }
        if (is_blocked(path)) { errno = ENOENT; return nullptr; }
    }
    return orig_fopen ? orig_fopen(path, mode) : nullptr;
}

static int my_open(const char* path, int flags, ...) {
    if (path && is_blocked(path)) { errno = ENOENT; return -1; }
    if (!orig_open) return -1;
    va_list ap; va_start(ap, flags);
    mode_t mode = (flags & O_CREAT) ? va_arg(ap, mode_t) : 0;
    va_end(ap);
    return (flags & O_CREAT) ? orig_open(path, flags, mode) : orig_open(path, flags);
}

static ssize_t my_readlink(const char* path, char* buf, size_t sz) {
    if (path && is_blocked(path)) { errno = ENOENT; return -1; }
    return orig_readlink ? orig_readlink(path, buf, sz) : -1;
}

static DIR* my_opendir(const char* name) {
    if (name && is_blocked(name)) { errno = ENOENT; return nullptr; }
    return orig_opendir ? orig_opendir(name) : nullptr;
}

/* ── install_file_hooks ── */
static void install_file_hooks() {
    void* libc = xdl_open("libc.so", XDL_DEFAULT);
    if (!libc) { LOGE("xdl_open(libc.so) failed"); return; }

#define TRY_HOOK(name, new_fn, orig_pp)                                    \
    do {                                                                     \
        void* _s = xdl_dsym(libc, name, nullptr);                          \
        if (!_s) _s = xdl_dsym(libc, "__" name, nullptr);                 \
        if (_s) {                                                            \
            if (DobbyHook(_s, (void*)(new_fn), (void**)(orig_pp)) == 0)   \
                LOGD("Hooked: " name);                                      \
            else LOGE("DobbyHook failed: " name);                          \
        } else { LOGE("Symbol not found: " name); }                        \
    } while(0)

    TRY_HOOK("access",   my_access,   &orig_access);
    TRY_HOOK("stat",     my_stat,     &orig_stat);
    TRY_HOOK("lstat",    my_lstat,    &orig_lstat);
    TRY_HOOK("fopen",    my_fopen,    &orig_fopen);
    TRY_HOOK("open",     my_open,     &orig_open);
    TRY_HOOK("readlink", my_readlink, &orig_readlink);
    TRY_HOOK("opendir",  my_opendir,  &orig_opendir);

#undef TRY_HOOK

    xdl_close(libc);
    LOGD("install_file_hooks: complete");
}

__attribute__((constructor))
void install_antidetection_hooks() {
    LOGD("Installing AntiDetection + FakeRoot + GGBypass hooks");
    install_file_hooks();
    LOGD("AntiDetection ready");
}
