package view;

import com.github.vatbub.common.updater.HidableUpdateProgressDialog;

public interface HidableProgressDialogWithEnqueuedNotification extends HidableUpdateProgressDialog {
    void enqueued();
}
