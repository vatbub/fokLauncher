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
import org.jdom2.JDOMException;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.logging.Level;

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

                Version versionToDownload;

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

                boolean cont = true;

                if (!getCurrentEntry().getApp().isPresentOnHarddrive(versionToDownload)) {
                    cont = getCurrentEntry().getApp().download(versionToDownload, getCurrentEntry().getGui());
                }

                // Execute only if not cancelled by user
                if (cont && getCurrentEntry().isLaunchAfterDownload()) {
                    getCurrentEntry().getApp().launch(getCurrentEntry().getGui(), versionToDownload, getCurrentEntry().getStartupArgs());
                } else if (getCurrentEntry().getGui() != null) {
                    getCurrentEntry().getGui().hide();
                }
            } catch (IOException | JDOMException | NoSuchElementException e) {
                getCurrentEntry().getGui().hide();
                FOKLogger.log(DownloadThread.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
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
