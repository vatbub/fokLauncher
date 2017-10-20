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
import com.github.vatbub.common.updater.HidableUpdateProgressDialog;

public class CLIProgressUpdateDialog implements HidableUpdateProgressDialog {
    @Override
    public void hide() {
        // do nothing
    }

    @Override
    public void preparePhaseStarted() {
        FOKLogger.info(getClass().getName(), "Preparing...");
    }

    @Override
    public void downloadStarted() {
        FOKLogger.info(getClass().getName(), "Downloading...");
    }

    @Override
    public void downloadProgressChanged(double kilobytesDownloaded, double totalFileSizeInKB) {
        System.out.print(Math.round((kilobytesDownloaded / totalFileSizeInKB) * 100) + "%\r");
    }

    @Override
    public void installStarted() {
        System.out.println();
        FOKLogger.info(getClass().getName(), "Installing...");
    }

    @Override
    public void launchStarted() {
        FOKLogger.info(getClass().getName(), "Launching...");
    }

    @Override
    public void cancelRequested() {
        FOKLogger.info(getClass().getName(), "Cancelling");
    }

    @Override
    public void operationCanceled() {
        FOKLogger.info(getClass().getName(), "Operation cancelled");
    }

    @Override
    public void showErrorMessage(String s) {
        FOKLogger.severe(getClass().getName(), "Something went wrong: " + s);
    }
}
