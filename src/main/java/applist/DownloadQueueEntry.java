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


import com.github.vatbub.common.updater.Version;
import view.HidableProgressDialogWithEnqueuedNotification;

public class DownloadQueueEntry {
    private App app;
    private HidableProgressDialogWithEnqueuedNotification gui;
    private Version versionToDownload;
    private boolean enableSnapshots;
    private boolean launchAfterDownload;

    public DownloadQueueEntry() {
        this(null);
    }

    public DownloadQueueEntry(App app) {
        this(app, false);
    }

    public DownloadQueueEntry(App app, boolean enableSnapshots) {
        this(app, null, null, enableSnapshots);
    }

    public DownloadQueueEntry(App app, Version versionToDownload) {
        this(app, null, versionToDownload);
    }

    public DownloadQueueEntry(App app, HidableProgressDialogWithEnqueuedNotification gui) {
        this(app, gui, null);
    }

    public DownloadQueueEntry(App app, HidableProgressDialogWithEnqueuedNotification gui, boolean enableSnapshots) {
        this(app, gui, null, enableSnapshots);
    }

    public DownloadQueueEntry(App app, HidableProgressDialogWithEnqueuedNotification gui, Version versionToDownload) {
        this(app, gui, versionToDownload, false);
    }

    public DownloadQueueEntry(App app, HidableProgressDialogWithEnqueuedNotification gui, Version versionToDownload, boolean enableSnapshots) {
        setApp(app);
        setGui(gui);
        setEnableSnapshots(enableSnapshots);
        setVersionToDownload(versionToDownload);
    }

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }

    public HidableProgressDialogWithEnqueuedNotification getGui() {
        return gui;
    }

    public void setGui(HidableProgressDialogWithEnqueuedNotification gui) {
        this.gui = gui;
    }

    public Version getVersionToDownload() {
        return versionToDownload;
    }

    public void setVersionToDownload(Version versionToDownload) {
        this.versionToDownload = versionToDownload;
    }

    public boolean isEnableSnapshots() {
        return enableSnapshots;
    }

    public void setEnableSnapshots(boolean enableSnapshots) {
        this.enableSnapshots = enableSnapshots;
    }

    public boolean isLaunchAfterDownload() {
        return launchAfterDownload;
    }

    public void setLaunchAfterDownload(boolean launchAfterDownload) {
        this.launchAfterDownload = launchAfterDownload;
    }
}
