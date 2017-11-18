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
import config.TestSuperClass;
import org.junit.Assert;
import org.junit.Test;
import view.HidableProgressDialogWithEnqueuedNotification;
import view.TestProgressView;

public class DownloadQueueEntryTest extends TestSuperClass {
    HidableProgressDialogWithEnqueuedNotification testGUI = new TestProgressView();
    String[] testStartupArgs = {"arg1", "arg2"};
    private App testApp = new App("testApp");
    private Version testVersion = new Version("0.0.1");

    @Test
    public void allParameterConstructorTest() {
        // snapshots enabled
        DownloadQueueEntry entry = new DownloadQueueEntry(testApp, testGUI, testVersion, true, testStartupArgs);
        assertEntry(entry, testApp, testGUI, testVersion, true, testStartupArgs);

        // snapshots disabled
        entry = new DownloadQueueEntry(testApp, testGUI, testVersion, false, testStartupArgs);
        assertEntry(entry, testApp, testGUI, testVersion, false, testStartupArgs);
    }

    @Test
    public void noVersionParameterConstructorTest() {
        // snapshots enabled
        DownloadQueueEntry entry = new DownloadQueueEntry(testApp, testGUI, true, testStartupArgs);
        assertEntry(entry, testApp, testGUI, null, true, testStartupArgs);

        // snapshots disabled
        entry = new DownloadQueueEntry(testApp, testGUI, false, testStartupArgs);
        assertEntry(entry, testApp, testGUI, null, false, testStartupArgs);
    }

    private void assertEntry(DownloadQueueEntry entry, App app, HidableProgressDialogWithEnqueuedNotification gui, Version version, boolean snapshotsEnabled, String[] startupArgs) {
        Assert.assertTrue(app == entry.getApp());
        Assert.assertTrue(gui == entry.getGui());
        Assert.assertTrue(version == entry.getVersionToDownload());
        Assert.assertEquals(snapshotsEnabled, entry.isEnableSnapshots());
        Assert.assertTrue(startupArgs == entry.getStartupArgs());
    }

    @Test
    public void noEnableSnapshotsParameterConstructorTest() {
        // snapshots enabled
        DownloadQueueEntry entry = new DownloadQueueEntry(testApp, testGUI, testVersion, testStartupArgs);
        assertEntry(entry, testApp, testGUI, testVersion, false, testStartupArgs);
    }

    @Test
    public void noEnableSnapshotsAndNoVersionParameterConstructorTest() {
        // snapshots enabled
        DownloadQueueEntry entry = new DownloadQueueEntry(testApp, testGUI, testStartupArgs);
        assertEntry(entry, testApp, testGUI, null, false, testStartupArgs);
    }

    @Test
    public void noEnableSnapshotsAndNoGUIParameterConstructorTest() {
        // snapshots enabled
        DownloadQueueEntry entry = new DownloadQueueEntry(testApp, testVersion, testStartupArgs);
        assertEntry(entry, testApp, null, testVersion, false, testStartupArgs);
    }

    @Test
    public void onlyAppAndEnableSnapshotParameterConstructorTest() {
        // snapshots enabled
        DownloadQueueEntry entry = new DownloadQueueEntry(testApp, true, testStartupArgs);
        assertEntry(entry, testApp, null, null, true, testStartupArgs);

        entry = new DownloadQueueEntry(testApp, false, testStartupArgs);
        assertEntry(entry, testApp, null, null, false, testStartupArgs);
    }

    @Test
    public void onlyAppParameterConstructorTest() {
        // snapshots enabled
        DownloadQueueEntry entry = new DownloadQueueEntry(testApp, testStartupArgs);
        assertEntry(entry, testApp, null, null, false, testStartupArgs);
    }

    @Test
    public void onlyStartupArgsParameterConstructorTest() {
        // snapshots enabled
        DownloadQueueEntry entry = new DownloadQueueEntry(testStartupArgs);
        assertEntry(entry, null, null, null, false, testStartupArgs);
    }

    @Test
    public void defaultConstructorTest() {
        // snapshots enabled
        DownloadQueueEntry entry = new DownloadQueueEntry();

        Assert.assertTrue(null == entry.getApp());
        Assert.assertTrue(null == entry.getGui());
        Assert.assertTrue(null == entry.getVersionToDownload());
        Assert.assertEquals(false, entry.isEnableSnapshots());
        Assert.assertEquals(0, entry.getStartupArgs().length);
    }

    @Test
    public void launchAfterDownloadTest() {
        DownloadQueueEntry entry = new DownloadQueueEntry(testApp, testGUI, testVersion, false, testStartupArgs);

        Assert.assertEquals(false, entry.isLaunchAfterDownload());

        entry.setLaunchAfterDownload(true);
        Assert.assertEquals(true, entry.isLaunchAfterDownload());

        entry.setLaunchAfterDownload(false);
        Assert.assertEquals(false, entry.isLaunchAfterDownload());
    }
}
