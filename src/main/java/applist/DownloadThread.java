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


import org.jdom2.JDOMException;

import java.io.IOException;

public class DownloadThread extends Thread {
    private static int downloadThreadCounter = 0;
    private boolean shutdownAfterDownload;
    private DownloadQueue queue;

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

    @Override
    public void run() {
        while (getQueue().getCurrentQueueCount() > 0 && !isShutdownAfterDownload()) {
            DownloadQueueEntry entry = getQueue().removeFirst();
            try {
                if (entry.getVersionToDownload() == null) {
                    // download latest
                    entry.getApp().download(entry.getGui());
                } else {
                    entry.getApp().download(entry.getVersionToDownload(), entry.getGui());
                }
            } catch (IOException | JDOMException e) {
                e.printStackTrace();
            }
        }
    }

    public void setShutdownAfterDownload(boolean shutdownAfterDownload) {
        this.shutdownAfterDownload = shutdownAfterDownload;
    }

    public DownloadQueue getQueue() {
        return queue;
    }

    public void setQueue(DownloadQueue queue) {
        this.queue = queue;
    }
}
