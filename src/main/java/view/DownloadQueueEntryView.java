package view;

import applist.App;
import com.github.vatbub.common.updater.HidableUpdateProgressDialog;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class DownloadQueueEntryView extends HBox implements HidableUpdateProgressDialog {
    private MainWindow mainWindow;
    private ListView<DownloadQueueEntryView> parent;
    private ProgressBar progressBar;
    private Label progressLabel;
    private Button cancelButton;
    private App app;
    private ImageView cancelButtonIcon = new ImageView(new Image(DownloadQueueEntryView.class.getResourceAsStream("cancel.png")));

    public DownloadQueueEntryView(MainWindow mainWindow, ListView<DownloadQueueEntryView> parent, App app) {
        this(mainWindow, parent, app, 0);
    }

    public DownloadQueueEntryView(MainWindow mainWindow, ListView<DownloadQueueEntryView> parent, App app, double spacing) {
        super(spacing);
        setMainWindow(mainWindow);
        setParent(parent);
        setApp(app);
        buildViewAndAttachToParent();
    }

    private void buildViewAndAttachToParent() {
        setAlignment(Pos.CENTER_LEFT);
        setHgrow(this, Priority.ALWAYS);
        progressBar = new ProgressBar(-1);
        progressLabel = new Label();
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
        this.getChildren().addAll(progressBar, progressLabel, cancelButton);
        getParentCustom().getItems().add(this);
    }

    @Override
    public void hide() {
        Platform.runLater(() -> getParentCustom().getItems().remove(this));
    }

    @Override
    public void preparePhaseStarted() {
        Platform.runLater(() -> {
            progressBar.setProgress(-1);
            progressLabel.setText(MainWindow.getBundle().getString("progress.preparing"));
        });
    }

    @Override
    public void downloadStarted() {
        Platform.runLater(() -> {
            progressBar.setProgress(-1);
            progressLabel.setText(MainWindow.getBundle().getString("progress.downloading"));
        });
    }

    @Override
    public void downloadProgressChanged(double kilobytesDownloaded, double totalKiloBytes) {
        Platform.runLater(() -> {
            progressBar.setProgress(kilobytesDownloaded / totalKiloBytes);
            progressLabel.setText(MainWindow.getBundle().getString("progress.downloading"));
        });
    }

    @Override
    public void installStarted() {
        Platform.runLater(() -> {
            progressBar.setProgress(-1);
            progressLabel.setText(MainWindow.getBundle().getString("progress.installing"));
        });
    }

    @Override
    public void launchStarted() {
        Platform.runLater(() -> {
            progressBar.setProgress(-1);
            progressLabel.setText(MainWindow.getBundle().getString("progress.launching"));
        });
    }

    @Override
    public void cancelRequested() {
        Platform.runLater(() -> {
            progressBar.setProgress(-1);
            progressLabel.setText(MainWindow.getBundle().getString("cancelRequested"));
            cancelButton.setDisable(true);
        });
    }

    @Override
    public void operationCanceled() {
        // do nothing
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
}
