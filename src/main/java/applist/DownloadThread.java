package applist;

/*-
 * #%L
 * FOK Launcher
 * %%
 * Copyright (C) 2016 - 2017 Frederik Kammel
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import com.github.vatbub.common.core.logging.FOKLogger;
import com.github.vatbub.common.updater.Version;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import static org.awaitility.Awaitility.await;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class DownloadThread extends Thread {
    private static int downloadThreadCounter = 0;
    private boolean shutdownAfterDownload;
    private DownloadQueue queue;
    private volatile DownloadQueueEntry currentEntry;

    public DownloadThread(DownloadQueue queue) {
        this(null, queue);
    }

    public DownloadThread(ThreadGroup group, DownloadQueue queue) {
        this(group, queue, "DownloadThread-" + getNextDownloadTreadCounter());
    }

    public DownloadThread(DownloadQueue queue, String name) {
        this(null, queue, name);
    }

    public DownloadThread(ThreadGroup group, DownloadQueue queue, String name) {
        this(group, queue, name, 0);
    }

    public DownloadThread(ThreadGroup group, DownloadQueue queue, String name, long stackSize) {
        super(group, null, name, stackSize);
        setQueue(queue);
    }

    private static synchronized int getNextDownloadTreadCounter() {
        return downloadThreadCounter++;
    }

    public boolean isShutdownAfterDownload() {
        return shutdownAfterDownload;
    }

    public void setShutdownAfterDownload(boolean shutdownAfterDownload) {
        this.shutdownAfterDownload = shutdownAfterDownload;
    }

    @Override
    public void run() {
        while (getQueue().getCurrentQueueCount() > 0 && !isShutdownAfterDownload()) {
            try {
                setCurrentEntry(getQueue().removeFirst());
                boolean cont = true;

                Version versionToDownload = getCurrentEntry().getApp().getCurrentlyInstalledVersion(getCurrentEntry().isEnableSnapshots());
                try {
                    if (!getCurrentEntry().isDisableDownload()) {
                        if (getCurrentEntry().getVersionToDownload() == null) {
                            // download latest
                            if (getCurrentEntry().isEnableSnapshots()) {
                                versionToDownload = getCurrentEntry().getApp().getLatestOnlineSnapshotVersion();
                            } else {
                                versionToDownload = getCurrentEntry().getApp().getLatestOnlineVersion();
                            }
                        } else {
                            versionToDownload = getCurrentEntry().getVersionToDownload();
                        }

                        Version finalVersionToDownload = versionToDownload;
                        await().until(() -> !getCurrentEntry().getApp().getLockFile(finalVersionToDownload).isLocked());

                        if (!getCurrentEntry().getApp().isPresentOnHardDrive(versionToDownload)) {
                            cont = getCurrentEntry().getApp().download(versionToDownload, getCurrentEntry().getGui());
                        }
                    }
                } catch (UnknownHostException e) {
                    FOKLogger.log(DownloadThread.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
                }

                // Execute only if not cancelled by user
                if (cont) {
                    if (getCurrentEntry().isLaunchAfterDownload()) {
                        final DownloadQueueEntry currentEntryCopy = getCurrentEntry();
                        final Version finalVersionToDownload = versionToDownload;
                        new Thread(() -> {
                            try {
                                currentEntryCopy.getApp().launch(currentEntryCopy.getGui(), finalVersionToDownload, currentEntryCopy.getStartupArgs());
                            } catch (IOException e) {
                                FOKLogger.log(DownloadThread.class.getName(), Level.SEVERE, "Unable to launch the app", e);
                                if (currentEntryCopy.getGui() != null) {
                                    currentEntryCopy.getGui().showErrorMessage("Unable to launch the app " + currentEntryCopy.getApp().getName() + "\n" + ExceptionUtils.getStackTrace(e));
                                }
                            }
                        }).start();
                    }

                    if (getCurrentEntry().getGui() != null) {
                        getCurrentEntry().getGui().hide();
                    }
                }
            } catch (NoSuchElementException e) {
                FOKLogger.log(DownloadThread.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
            } catch (Exception e) {
                FOKLogger.log(DownloadThread.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
                getCurrentEntry().getGui().hide();
                getCurrentEntry().getApp().fireLaunchedAppExits();
                getCurrentEntry().getGui().showErrorMessage(e.toString());
            } finally {
                setCurrentEntry(null);
            }
        }
    }

    public DownloadQueue getQueue() {
        return queue;
    }

    public void setQueue(DownloadQueue queue) {
        this.queue = queue;
    }

    public boolean isBusy() {
        return getCurrentEntry() != null;
    }

    public DownloadQueueEntry getCurrentEntry() {
        return currentEntry;
    }

    private void setCurrentEntry(DownloadQueueEntry currentEntry) {
        this.currentEntry = currentEntry;
        getQueue().updateQueueCount();
    }
}
