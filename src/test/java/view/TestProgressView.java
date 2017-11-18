package view;

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
import org.junit.Test;

/**
 * The purpose of this implementation of {@link HidableProgressDialogWithEnqueuedNotification} is to be able to
 * verify if gui methods are called.
 * The class is a no-op except that one can check afterwards if methods were called and what the passed arguments were.
 */
public class TestProgressView implements HidableProgressDialogWithEnqueuedNotification {
    private boolean enqueuedCalled;
    private boolean hideCalled;
    private boolean preparePhaseStartedCalled;
    private boolean downloadStartedCalled;
    private boolean downloadProgressChangedCalled;
    private double lastKBDownloaded;
    private double lastTotalKB;
    private boolean installStartedCalled;
    private boolean launchStartedCalled;
    private boolean cancelRequestedCalled;
    private boolean operationCancelledCalled;
    private String lastErrorMessage;
    private boolean showErrorMessageCalled;

    @Test
    public void dummyTest(){
        // we need at least one test per class in the test folder, otherwise maven builds will fail
        FOKLogger.info(TestProgressView.class.getName(), "Yeeha!");
    }

    @Override
    public void enqueued() {
        enqueuedCalled = true;
    }

    @Override
    public void hide() {
        hideCalled = true;
    }

    @Override
    public void preparePhaseStarted() {
        preparePhaseStartedCalled = true;
    }

    @Override
    public void downloadStarted() {
        downloadStartedCalled = true;
    }

    @Override
    public void downloadProgressChanged(double kbDownloaded, double totalKB) {
        downloadProgressChangedCalled = true;
        lastKBDownloaded = kbDownloaded;
        lastTotalKB = totalKB;
    }

    @Override
    public void installStarted() {
        installStartedCalled = true;
    }

    @Override
    public void launchStarted() {
        launchStartedCalled = true;
    }

    @Override
    public void cancelRequested() {
        cancelRequestedCalled = true;
    }

    @Override
    public void operationCanceled() {
        operationCancelledCalled = true;
    }

    @Override
    public void showErrorMessage(String s) {
        showErrorMessageCalled = true;
        lastErrorMessage = s;
    }

    public boolean isEnqueuedCalled() {
        return enqueuedCalled;
    }

    public boolean isHideCalled() {
        return hideCalled;
    }

    public boolean isPreparePhaseStartedCalled() {
        return preparePhaseStartedCalled;
    }

    public boolean isDownloadStartedCalled() {
        return downloadStartedCalled;
    }

    public boolean isDownloadProgressChangedCalled() {
        return downloadProgressChangedCalled;
    }

    public double getLastKBDownloaded() {
        return lastKBDownloaded;
    }

    public double getLastTotalKB() {
        return lastTotalKB;
    }

    public boolean isInstallStartedCalled() {
        return installStartedCalled;
    }

    public boolean isLaunchStartedCalled() {
        return launchStartedCalled;
    }

    public boolean isCancelRequestedCalled() {
        return cancelRequestedCalled;
    }

    public boolean isOperationCancelledCalled() {
        return operationCancelledCalled;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public boolean isShowErrorMessageCalled() {
        return showErrorMessageCalled;
    }
}
