// SPDX-FileCopyrightText: 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.core;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.osfans.trime.TrimeService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 词库自动同步监视器。
 *
 * <p>轮询 Rime sync 目录中“其他安装”（installation_id 目录）的用户词库快照
 * （*.userdb.txt）的变化，检测到远端更新后自动触发 {@link Rime#syncUserData()}，
 * 配合电脑端小狼毫的自动同步实现手机/电脑词库一致。</p>
 *
 * <p>使用轮询而非 {@link android.os.FileObserver}：Android 10+ 分区存储的 FUSE
 * 下，FileObserver 收不到其他应用对公共目录的写入事件。</p>
 *
 * <p>自身同步写入的快照位于自己的 installation_id 目录，已从监听中排除，
 * 不会造成同步回环。</p>
 */
public class SyncMonitor {

    private static final String TAG = "SyncMonitor";

    /** 快照文件后缀 */
    private static final String SNAPSHOT_SUFFIX = ".userdb.txt";

    /** 轮询间隔 */
    private static final long POLL_INTERVAL_SECONDS = 10;

    /** 远端快照更新后的防抖延迟，等待同步工具完成文件写入 */
    private static final long DEBOUNCE_MILLIS = 5000;

    private final TrimeService service;
    private final Rime rime;
    private final Handler handler;

    private File syncDir;
    private String myInstallationId = "";
    private final Map<String, String> snapshotFingerprints = new HashMap<>();
    private volatile boolean started;
    private volatile boolean syncPending;
    private ScheduledExecutorService poller;

    private final Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            syncPending = false;
            if (!started || service == null || rime == null) {
                return;
            }
            Log.i(TAG, "remote sync snapshot updated, auto syncing user data");
            rime.syncUserData();
            logFile("auto sync triggered, remote snapshot updated");
        }
    };

    private final Runnable pollTask = new Runnable() {
        @Override
        public void run() {
            if (!started) {
                return;
            }
            try {
                boolean changed = pollSnapshots();
                if (changed) {
                    scheduleSync();
                }
            } catch (Exception e) {
                Log.w(TAG, "poll failed", e);
            }
        }
    };

    public SyncMonitor(TrimeService service, Rime rime, Handler handler) {
        this.service = service;
        this.rime = rime;
        this.handler = handler;
    }

    /**
     * 启动监听。Rime 引擎初始化完成后调用。
     */
    public synchronized void start() {
        logFile("start() called, started=" + started);
        if (started) {
            return;
        }
        myInstallationId = readInstallationId();
        logFile("installation id: " + myInstallationId);
        syncDir = resolveSyncDir();
        logFile("sync dir: " + syncDir);
        if (syncDir == null) {
            Log.w(TAG, "sync dir unavailable, auto sync disabled");
            return;
        }
        started = true;
        poller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sync-monitor-poll");
            t.setDaemon(true);
            return t;
        });
        poller.scheduleWithFixedDelay(pollTask, POLL_INTERVAL_SECONDS,
                POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
        Log.i(TAG, "sync monitor started, sync dir: " + syncDir
                + ", installation id: " + myInstallationId);
        logFile("start() done, sync dir: " + syncDir);
    }

    private void logFile(String msg) {
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(
                new File(DataManager.getUserDataDir(), "syncmonitor.log"), true)) {
            fos.write((System.currentTimeMillis() + " " + msg + "\n").getBytes());
        } catch (IOException e) {
        }
    }

    /**
     * 停止监听。
     */
    public synchronized void stop() {
        if (!started) {
            return;
        }
        started = false;
        handler.removeCallbacks(syncRunnable);
        if (poller != null) {
            poller.shutdownNow();
            poller = null;
        }
        Log.i(TAG, "sync monitor stopped");
    }

    /**
     * 手动请求一次同步（造词完成后调用），带防抖。
     */
    public void requestSync() {
        scheduleSync();
    }

    private void scheduleSync() {
        logFile("scheduleSync() called, started=" + started + " syncPending=" + syncPending);
        if (!started || syncPending) {
            Log.d(TAG, "scheduleSync skipped: started=" + started
                    + " syncPending=" + syncPending);
            return;
        }
        syncPending = true;
        handler.removeCallbacks(syncRunnable);
        handler.postDelayed(syncRunnable, DEBOUNCE_MILLIS);
        Log.i(TAG, "sync scheduled in " + DEBOUNCE_MILLIS + "ms");
    }

    /**
     * 检查所有其他安装目录中的快照文件指纹（修改时间+大小），
     * 返回是否有变化。
     */
    private boolean pollSnapshots() {
        if (syncDir == null || !syncDir.isDirectory()) {
            return false;
        }
        File[] dirs = syncDir.listFiles(File::isDirectory);
        if (dirs == null) {
            return false;
        }
        boolean changed = false;
        Set<String> seen = new HashSet<>();
        for (File dir : dirs) {
            String name = dir.getName();
            if (!TextUtils.isEmpty(myInstallationId) && name.equals(myInstallationId)) {
                continue;
            }
            seen.add(name);
            File[] snaps = dir.listFiles((d, f) -> f.endsWith(SNAPSHOT_SUFFIX));
            if (snaps == null) {
                continue;
            }
            for (File snap : snaps) {
                String key = name + "/" + snap.getName();
                String fingerprint = fingerprint(snap);
                String prev = snapshotFingerprints.get(key);
                if (prev != null && !prev.equals(fingerprint)) {
                    logFile("snapshot changed: " + key);
                    changed = true;
                }
                snapshotFingerprints.put(key, fingerprint);
            }
        }
        snapshotFingerprints.keySet().removeIf(key -> !seen.contains(key.split("/")[0]));
        return changed;
    }

    private String fingerprint(File f) {
        try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
            byte[] buf = new byte[8192];
            long h = 0xcbf29ce484222325L;
            int n;
            while ((n = in.read(buf)) != -1) {
                for (int i = 0; i < n; i++) {
                    h ^= buf[i] & 0xff;
                    h *= 0x100000001b3L;
                }
            }
            return Long.toHexString(h);
        } catch (IOException e) {
            return "err:" + f.lastModified();
        }
    }

    /**
     * 解析安装 ID：&lt;userDataDir&gt;/installation.yaml 中的 installation_id。
     */
    private String readInstallationId() {
        File installationInfo = new File(DataManager.getUserDataDir(), "installation.yaml");
        if (!installationInfo.exists()) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(installationInfo))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("installation_id:")) {
                    return trimmed.substring("installation_id:".length()).trim()
                            .replace("\"", "").replace("'", "");
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "failed to read installation.yaml", e);
        }
        return "";
    }

    /**
     * 解析 sync 目录：优先 installation.yaml 的 sync_dir，否则 &lt;userDataDir&gt;/sync。
     */
    private File resolveSyncDir() {
        File installationInfo = new File(DataManager.getUserDataDir(), "installation.yaml");
        if (installationInfo.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(installationInfo))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("sync_dir:")) {
                        String dir = trimmed.substring("sync_dir:".length()).trim()
                                .replace("\"", "").replace("'", "");
                        if (!TextUtils.isEmpty(dir)) {
                            return new File(dir);
                        }
                    }
                }
            } catch (IOException e) {
                Log.w(TAG, "failed to read sync_dir from installation.yaml", e);
            }
        }
        File defaultDir = new File(DataManager.getUserDataDir(), "sync");
        defaultDir.mkdirs();
        return defaultDir;
    }
}
