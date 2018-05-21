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


import com.github.vatbub.common.core.StringCommon;
import com.github.vatbub.common.core.logging.FOKLogger;

import java.util.ArrayList;
import java.util.List;

public class CLIProgressUpdateDialog implements HidableProgressDialogWithEnqueuedNotification {
    private static final CLIDownloadProgressDisplayManager cliDownloadProgressDisplayManager = new CLIDownloadProgressDisplayManager();
    private double kilobytesDownloaded;
    private double totalFileSizeInKB;

    @Override
    public void hide() {
        cliDownloadProgressDisplayManager.deregister(this);
        cliDownloadProgressDisplayManager.clearLine();
        FOKLogger.info(getClass().getName(), "Finished!");
    }

    @Override
    public void preparePhaseStarted() {
        cliDownloadProgressDisplayManager.deregister(this);
        cliDownloadProgressDisplayManager.clearLine();
        FOKLogger.info(getClass().getName(), "Preparing...");
    }

    @Override
    public void downloadStarted() {
        cliDownloadProgressDisplayManager.clearLine();
        FOKLogger.info(getClass().getName(), "Downloading...");
        cliDownloadProgressDisplayManager.register(this);
    }

    @Override
    public void downloadProgressChanged(double kilobytesDownloaded, double totalFileSizeInKB) {
        this.kilobytesDownloaded = kilobytesDownloaded;
        this.totalFileSizeInKB = totalFileSizeInKB;
        cliDownloadProgressDisplayManager.updateProgress();
    }

    @Override
    public void installStarted() {
        cliDownloadProgressDisplayManager.deregister(this);
        cliDownloadProgressDisplayManager.clearLine();
        FOKLogger.info(getClass().getName(), "Installing...");
    }

    @Override
    public void launchStarted() {
        cliDownloadProgressDisplayManager.deregister(this);
        cliDownloadProgressDisplayManager.clearLine();
        FOKLogger.info(getClass().getName(), "Launching...");
    }

    @Override
    public void cancelRequested() {
        cliDownloadProgressDisplayManager.deregister(this);
        cliDownloadProgressDisplayManager.clearLine();
        FOKLogger.info(getClass().getName(), "Cancelling");
    }

    @Override
    public void operationCanceled() {
        cliDownloadProgressDisplayManager.deregister(this);
        cliDownloadProgressDisplayManager.clearLine();
        FOKLogger.info(getClass().getName(), "Operation cancelled");
    }

    @Override
    public void showErrorMessage(String s) {
        cliDownloadProgressDisplayManager.deregister(this);
        cliDownloadProgressDisplayManager.clearLine();
        FOKLogger.severe(getClass().getName(), "Something went wrong: " + s);
    }

    @Override
    public void enqueued() {
        cliDownloadProgressDisplayManager.deregister(this);
        cliDownloadProgressDisplayManager.clearLine();
        FOKLogger.info(getClass().getName(), "Download is enqueued...");
    }

    public double getKilobytesDownloaded() {
        return kilobytesDownloaded;
    }

    public double getTotalFileSizeInKB() {
        return totalFileSizeInKB;
    }

    private static class CLIDownloadProgressDisplayManager {
        private final List<CLIProgressUpdateDialog> cliProgressUpdateDialogs = new ArrayList<>();
        private int maxLength = 0;

        public void register(CLIProgressUpdateDialog cliProgressUpdateDialog) {
            if (!cliProgressUpdateDialogs.contains(cliProgressUpdateDialog))
                cliProgressUpdateDialogs.add(cliProgressUpdateDialog);
        }

        public void deregister(CLIProgressUpdateDialog cliProgressUpdateDialog) {
            if (!cliProgressUpdateDialogs.contains(cliProgressUpdateDialog))
                return;

            cliProgressUpdateDialogs.remove(cliProgressUpdateDialog);
            updateProgress();
        }

        public void updateProgress() {
            synchronized (cliProgressUpdateDialogs) {
                StringBuilder finalString = new StringBuilder();
                if (cliProgressUpdateDialogs.size() != 0) {
                    finalString.append("Progress (").append(cliProgressUpdateDialogs.size()).append("): ");

                    for (int i = 0; i < cliProgressUpdateDialogs.size(); i++) {
                        finalString.append(StringCommon.convertFileSizeToReadableString(cliProgressUpdateDialogs.get(i).getKilobytesDownloaded()));
                        finalString.append(" / ");
                        finalString.append(StringCommon.convertFileSizeToReadableString(cliProgressUpdateDialogs.get(i).getTotalFileSizeInKB()));

                        if (i != cliProgressUpdateDialogs.size() - 1)
                            finalString.append(" | ");
                    }

                    if (finalString.length() > maxLength)
                        maxLength = finalString.length();
                }

                // append spaces to clear the rest of the line
                int l = finalString.length();
                for (int i = 0; i <= maxLength - l; i++)
                    finalString.append(" ");

                finalString.append("\r");
                System.out.print(finalString);
            }
        }

        public void clearLine(){
            StringBuilder finalString = new StringBuilder();
            for (int i = 0; i <= maxLength; i++)
                finalString.append(" ");

            finalString.append("\r");
            System.out.print(finalString);
        }
    }
}
