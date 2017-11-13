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


import applist.App;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class DownloadQueueEntryView extends AnchorPane implements HidableProgressDialogWithEnqueuedNotification {
    private MainWindow mainWindow;
    private ListView<DownloadQueueEntryView> parent;
    private ProgressBar progressBar;
    private Label progressLabel;
    private Button cancelButton;
    private App app;
    private ImageView cancelButtonIcon = new ImageView(new Image(DownloadQueueEntryView.class.getResourceAsStream("cancel.png")));
    private volatile DownloadStatus currentStatus;
    private double kilobytesDownloaded;
    private double totalKiloBytes;
    private HidableProgressDialogWithEnqueuedNotification attachedGui = null;

    public DownloadQueueEntryView(MainWindow mainWindow, ListView<DownloadQueueEntryView> parent, App app) {
        // super(spacing);
        setMainWindow(mainWindow);
        setParent(parent);
        setApp(app);
        buildViewAndAttachToParent();
    }

    private void buildViewAndAttachToParent() {
        // setAlignment(Pos.CENTER_LEFT);
        // setHgrow(this, Priority.ALWAYS);
        progressBar = new ProgressBar(-1);
        progressLabel = new Label();
        Label titleLabel = new Label(getApp().getName());
        Label spacerLabel = new Label(" - ");
        cancelButton = new Button("", cancelButtonIcon);
        cancelButton.getStyleClass().add("transparentButton");
        cancelButton.disableProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // disabled, select gray icon
                cancelButtonIcon.setImage(new Image(DownloadQueueEntryView.class.getResourceAsStream("cancel_gray.png")));
            } else {
                // enabled, select blue icon
                cancelButtonIcon.setImage(new Image(DownloadQueueEntryView.class.getResourceAsStream("cancel.png")));
            }
        });
        cancelButton.setOnAction((event -> getApp().cancelDownloadAndLaunch(this)));
        setBottomAnchor(progressBar, 0.0);
        setLeftAnchor(progressBar, 0.0);
        setRightAnchor(progressBar, 0.0);
        setTopAnchor(progressBar, 0.0);

        HBox subBox = new HBox(5, titleLabel, spacerLabel, progressLabel, cancelButton);
        HBox.setHgrow(subBox, Priority.ALWAYS);
        subBox.setAlignment(Pos.CENTER_RIGHT);

        setBottomAnchor(subBox, 0.0);
        setLeftAnchor(subBox, 0.0);
        setRightAnchor(subBox, 0.0);
        setTopAnchor(subBox, 0.0);

        this.getChildren().addAll(progressBar, subBox);
        getParentCustom().getItems().add(this);
    }

    @Override
    public void hide() {
        currentStatus=DownloadStatus.DONE;
        Platform.runLater(() -> getParentCustom().getItems().remove(this));
        if (getAttachedGui()!=null){
            getAttachedGui().hide();
        }
    }

    @Override
    public void enqueued() {
        currentStatus=DownloadStatus.ENQUEUED;
        Platform.runLater(() -> {
            progressBar.setProgress(-1);
            progressLabel.setText(MainWindow.getBundle().getString("progress.enqueued"));
        });

        if (getAttachedGui() != null) {
            getAttachedGui().enqueued();
        }
    }

    @Override
    public void preparePhaseStarted() {
        currentStatus=DownloadStatus.PREPARE_PHASE_STARTED;
        Platform.runLater(() -> {
            progressBar.setProgress(-1);
            progressLabel.setText(MainWindow.getBundle().getString("progress.preparing"));
        });

        if (getAttachedGui() != null) {
            getAttachedGui().preparePhaseStarted();
        }
    }

    @Override
    public void downloadStarted() {
        currentStatus=DownloadStatus.DOWNLOAD_STARTED;
        Platform.runLater(() -> {
            progressBar.setProgress(-1);
            progressLabel.setText(MainWindow.getBundle().getString("progress.downloading"));
        });

        if (getAttachedGui() != null) {
            getAttachedGui().downloadStarted();
        }
    }

    @Override
    public void downloadProgressChanged(double kilobytesDownloaded, double totalKiloBytes) {
        this.kilobytesDownloaded = kilobytesDownloaded;
        this.totalKiloBytes = totalKiloBytes;
        Platform.runLater(() -> {
            progressBar.setProgress(kilobytesDownloaded / totalKiloBytes);
            progressLabel.setText(MainWindow.getBundle().getString("progress.downloading"));
        });

        if (getAttachedGui() != null) {
            getAttachedGui().downloadProgressChanged(kilobytesDownloaded, totalKiloBytes);
        }
    }

    @Override
    public void installStarted() {
        currentStatus=DownloadStatus.INSTALL_STARTED;
        Platform.runLater(() -> {
            progressBar.setProgress(-1);
            progressLabel.setText(MainWindow.getBundle().getString("progress.installing"));
        });

        if (getAttachedGui() != null) {
            getAttachedGui().installStarted();
        }
    }

    @Override
    public void launchStarted() {
        currentStatus=DownloadStatus.LAUNCH_STARTED;
        Platform.runLater(() -> {
            progressBar.setProgress(-1);
            progressLabel.setText(MainWindow.getBundle().getString("progress.launching"));
        });

        if (getAttachedGui() != null) {
            getAttachedGui().launchStarted();
        }
    }

    @Override
    public void cancelRequested() {
        currentStatus=DownloadStatus.CANCEL_REQUESTED;
        Platform.runLater(() -> {
            progressBar.setProgress(-1);
            progressLabel.setText(MainWindow.getBundle().getString("cancelRequested"));
            cancelButton.setDisable(true);
        });

        if (getAttachedGui() != null) {
            getAttachedGui().cancelRequested();
        }
    }

    @Override
    public void operationCanceled() {
        currentStatus=DownloadStatus.CANCELLED;
        if (getAttachedGui() != null) {
            getAttachedGui().operationCanceled();
        }
    }

    @Override
    public void showErrorMessage(String s) {
        getMainWindow().showErrorMessage(s);
    }

    public MainWindow getMainWindow() {
        return mainWindow;
    }

    public void setMainWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    public ListView<DownloadQueueEntryView> getParentCustom() {
        return parent;
    }

    public void setParent(ListView<DownloadQueueEntryView> parent) {
        this.parent = parent;
    }

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }

    public DownloadStatus getCurrentStatus() {
        return currentStatus;
    }

    public double getKilobytesDownloaded() {
        return kilobytesDownloaded;
    }

    public double getTotalKiloBytes() {
        return totalKiloBytes;
    }

    public HidableProgressDialogWithEnqueuedNotification getAttachedGui() {
        return attachedGui;
    }

    public void setAttachedGui(HidableProgressDialogWithEnqueuedNotification attachedGui) {
        this.attachedGui = attachedGui;

        if (getCurrentStatus() == null)
            return;

        if (DownloadStatus.ENQUEUED.ordinal() <= getCurrentStatus().ordinal())
            attachedGui.enqueued();

        if (DownloadStatus.PREPARE_PHASE_STARTED.ordinal() <= getCurrentStatus().ordinal())
            attachedGui.preparePhaseStarted();

        if (DownloadStatus.DOWNLOAD_STARTED.ordinal() <= getCurrentStatus().ordinal()) {
            attachedGui.downloadStarted();
            attachedGui.downloadProgressChanged(kilobytesDownloaded, totalKiloBytes);
        }

        if (DownloadStatus.INSTALL_STARTED.ordinal() <= getCurrentStatus().ordinal())
            attachedGui.installStarted();

        if (DownloadStatus.LAUNCH_STARTED.ordinal() <= getCurrentStatus().ordinal())
            attachedGui.launchStarted();

        if (DownloadStatus.CANCEL_REQUESTED.ordinal() <= getCurrentStatus().ordinal())
            attachedGui.cancelRequested();

        if (DownloadStatus.CANCELLED.ordinal() <= getCurrentStatus().ordinal())
            attachedGui.operationCanceled();

        if (DownloadStatus.DONE.ordinal() <= getCurrentStatus().ordinal())
            attachedGui.hide();
    }

    public enum DownloadStatus {
        ENQUEUED, PREPARE_PHASE_STARTED, DOWNLOAD_STARTED, INSTALL_STARTED, LAUNCH_STARTED, CANCEL_REQUESTED, CANCELLED, DONE
    }
}
