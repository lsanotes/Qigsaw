/*
 * MIT License
 *
 * Copyright (c) 2019-present, iQIYI, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.iqiyi.android.qigsaw.core;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.os.Build;
import android.os.Looper;
import android.os.MessageQueue;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.android.play.core.splitcompat.SplitCompat;
import com.iqiyi.android.qigsaw.core.common.ProcessUtil;
import com.iqiyi.android.qigsaw.core.common.SplitBaseInfoProvider;
import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.common.SplitAABInfoProvider;
import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.extension.AABExtension;
import com.iqiyi.android.qigsaw.core.splitdownload.Downloader;
import com.iqiyi.android.qigsaw.core.splitinstall.SplitDownloaderManager;
import com.iqiyi.android.qigsaw.core.splitinstall.SplitInstallReporterManager;
import com.iqiyi.android.qigsaw.core.splitload.SplitLoadManager;
import com.iqiyi.android.qigsaw.core.splitload.SplitLoadManagerService;
import com.iqiyi.android.qigsaw.core.splitload.SplitLoadReporterManager;
import com.iqiyi.android.qigsaw.core.splitreport.DefaultSplitInstallReporter;
import com.iqiyi.android.qigsaw.core.splitreport.DefaultSplitLoadReporter;
import com.iqiyi.android.qigsaw.core.splitreport.DefaultSplitUpdateReporter;
import com.iqiyi.android.qigsaw.core.splitreport.SplitInstallReporter;
import com.iqiyi.android.qigsaw.core.splitreport.SplitLoadReporter;
import com.iqiyi.android.qigsaw.core.splitreport.SplitUpdateReporter;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitUpdateReporterManager;

import java.util.Set;

public class Qigsaw {

    /**
     * You are expected to install qigsaw in {@link android.app.Application#attachBaseContext(Context)} method.
     *
     * @param context    we will use the application context
     * @param downloader some apps have their own downloader, so qigsaw just provide interface of download operation.
     */
    public static void install(@NonNull Context context, @NonNull Downloader downloader) {
        install(context, downloader, null);
    }

    /**
     * You are expected to install qigsaw in {@link android.app.Application#attachBaseContext(Context)} method.
     *
     * @param context       we will use the application context
     * @param downloader    some apps have their own downloader, so qigsaw just provide interface of download operation.
     * @param configuration {@link SplitConfiguration}
     */
    public static void install(@NonNull Context context,
                               @NonNull Downloader downloader,
                               @Nullable SplitConfiguration configuration) {
        String manifestPackageName = null;
        String[] workProcesses = null;
        SplitInstallReporter installReporter = null;
        SplitLoadReporter loadReporter = null;
        SplitUpdateReporter updateReporter = null;
        SplitLog.Logger logger = null;
        if (configuration != null) {
            manifestPackageName = configuration.getManifestPackageName();
            workProcesses = configuration.getWorkProcesses();
            installReporter = configuration.getInstallReporter();
            loadReporter = configuration.getLoadReporter();
            updateReporter = configuration.getUpdateReporter();
            logger = configuration.getLogger();
        }
        if (TextUtils.isEmpty(manifestPackageName)) {
            manifestPackageName = context.getPackageName();
        }
        if (logger != null) {
            SplitLog.setSplitLogImp(logger);
        }
        final Context appContext = getApplicationContext(context);
        SplitBaseInfoProvider.setPackageName(manifestPackageName);
        //create AABCompat instance
        AABExtension.install(appContext);
        //create SplitLoadManager instance.
        SplitLoadManagerService.install(appContext);
        if (loadReporter == null) {
            loadReporter = new DefaultSplitLoadReporter(appContext);
        }
        SplitLoadReporterManager.install(loadReporter);
        SplitLoadManager loadManager = SplitLoadManagerService.getInstance();
        //load all installed splits for qigsaw.
        loadManager.load(workProcesses, !isSplitAppComponentFactoryExisting(appContext));
        //getInstance all installed splits for AAB.
        SplitAABInfoProvider infoProvider = new SplitAABInfoProvider(appContext);
        Set<String> loadedSplits = infoProvider.getInstalledSplitsForAAB();
        //if installed splits of aab are not empty, qigsaw would not work.
        if (loadedSplits.isEmpty()) {
            loadedSplits = loadManager.getLoadedSplitNames();
        }
        AABExtension.getInstance().onBaseContextAttached(loadedSplits);
        //only work in main process!
        if (ProcessUtil.isMainProcess(appContext)) {
            if (installReporter == null) {
                installReporter = new DefaultSplitInstallReporter(appContext);
            }
            if (updateReporter == null) {
                updateReporter = new DefaultSplitUpdateReporter(appContext);
            }
            SplitInstallReporterManager.install(installReporter);
            SplitUpdateReporterManager.install(updateReporter);
            SplitDownloaderManager.install(downloader);
            Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
                @Override
                public boolean queueIdle() {
                    cleanStaleSplits(appContext);
                    return false;
                }
            });
        }
        SplitCompat.install(appContext);
    }

    /**
     * Called when {@link Application#onCreate()} is invoked.
     */
    public static void onApplicationCreated() {
        AABExtension.getInstance().onCreate();
    }

    /**
     * Called when {@link Application#getResources()} is invoked.
     *
     * @param resources the return value of {@link Application#getResources()}.
     */
    public static void onApplicationGetResources(Resources resources) {
        if (SplitLoadManagerService.hasInstance() && resources != null) {
            SplitLoadManagerService.getInstance().getResources(resources);
        }
    }

    /**
     * Update split info version. If new split does not equal to current version, qigsaw would update it.
     *
     * @param context
     * @param newSplitInfoVersion new split info version.
     * @param newSplitInfoPath    path of new split info file.
     * @return if {@code true} start service successfully, otherwise {@code false}.
     */
    public static boolean updateSplits(Context context,
                                       @NonNull String newSplitInfoVersion,
                                       @NonNull String newSplitInfoPath) {
        try {
            Intent intent = new Intent();
            intent.setClassName(context, "com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitUpdateService");
            intent.putExtra(SplitConstants.NEW_SPLIT_INFO_VERSION, newSplitInfoVersion);
            intent.putExtra(SplitConstants.NEW_SPLIT_INFO_PATH, newSplitInfoPath);
            context.startService(intent);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Clean stale disk cache of all splits.
     */
    private static void cleanStaleSplits(Context context) {
        try {
            Intent intent = new Intent();
            intent.setClassName(context, "com.iqiyi.android.qigsaw.core.splitinstall.SplitCleanService");
            context.startService(intent);
        } catch (Exception e) {
            //ignored
        }
    }

    private static Context getApplicationContext(Context context) {
        Context appContext;
        if (context.getApplicationContext() == null) {
            appContext = context;
        } else {
            appContext = context.getApplicationContext();
        }
        return appContext;
    }

    private static boolean isSplitAppComponentFactoryExisting(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return false;
        }
        ApplicationInfo appInfo = context.getApplicationInfo();
        if (appInfo == null
                || appInfo.appComponentFactory == null) {
            return false;
        }
        if (appInfo.appComponentFactory.equals(SplitAppComponentFactory.class.getName())) {
            return true;
        }
        return isSubclassOfSplitAppComponentFactory(appInfo.appComponentFactory);
    }

    private static boolean isSubclassOfSplitAppComponentFactory(String className) {
        boolean ret = false;
        try {
            Class<?> originClazz = Class.forName(className);
            for (Class<?> clazz = originClazz; clazz != null; clazz = clazz.getSuperclass()) {
                if (clazz.getName().equals(SplitAppComponentFactory.class.getName())) {
                    ret = true;
                    break;
                }
            }
        } catch (ClassNotFoundException ignored) {

        }
        return ret;
    }

}
