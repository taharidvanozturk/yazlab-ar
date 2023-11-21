package com.google.ar.core.codelab.common.helpers;

import android.app.Activity;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;

/**
 * Snackbar'ı yönetmek için yardımcı sınıf. Android ile ilgili kodları gizler ve daha basit
 * yöntemleri ortaya çıkarır.
 */
public final class SnackbarHelper {
    private static final int BACKGROUND_COLOR = 0xbf323232;
    private Snackbar messageSnackbar;
    private enum DismissBehavior { HIDE, SHOW, FINISH }
    private int maxLines = 2;
    private String lastMessage = "";

    public boolean isShowing() {
        return messageSnackbar != null;
    }

    /** Belirli bir iletiyi içeren bir snackbar gösterir. */
    public void showMessage(Activity activity, String message) {
        if (!message.isEmpty() && (!isShowing() || !lastMessage.equals(message))) {
            lastMessage = message;
            show(activity, message, DismissBehavior.HIDE);
        }
    }

    /** Belirli bir iletiyi ve bir kapatma düğmesini içeren bir snackbar gösterir. */
    public void showMessageWithDismiss(Activity activity, String message) {
        show(activity, message, DismissBehavior.SHOW);
    }

    /**
     * Belirli bir hata iletiyi içeren bir snackbar gösterir. Kapatıldığında, etkinliği sonlandırır.
     * Hata durumlarını bildirmek için kullanışlıdır, etkinlikle başka etkileşim mümkün değildir.
     */
    public void showError(Activity activity, String errorMessage) {
        show(activity, errorMessage, DismissBehavior.FINISH);
    }

    /**
     * Şu anda görünen snackbar'ı gizler. Herhangi bir iplikten güvenli çağrılabilir. Snackbar gösterilmiyorsa
     * çağrılmışsa hiçbir şey yapmaz.
     */
    public void hide(Activity activity) {
        if (!isShowing()) {
            return;
        }
        lastMessage = "";
        Snackbar messageSnackbarToHide = messageSnackbar;
        messageSnackbar = null;
        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        messageSnackbarToHide.dismiss();
                    }
                });
    }

    public void setMaxLines(int lines) {
        maxLines = lines;
    }

    private void show(
            final Activity activity, final String message, final DismissBehavior dismissBehavior) {
        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        messageSnackbar =
                                Snackbar.make(
                                        activity.findViewById(android.R.id.content),
                                        message,
                                        Snackbar.LENGTH_INDEFINITE);
                        messageSnackbar.getView().setBackgroundColor(BACKGROUND_COLOR);
                        if (dismissBehavior != DismissBehavior.HIDE) {
                            messageSnackbar.setAction(
                                    "Kapat",
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            messageSnackbar.dismiss();
                                        }
                                    });
                            if (dismissBehavior == DismissBehavior.FINISH) {
                                messageSnackbar.addCallback(
                                        new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                                            @Override
                                            public void onDismissed(Snackbar transientBottomBar, int event) {
                                                super.onDismissed(transientBottomBar, event);
                                                activity.finish();
                                            }
                                        });
                            }
                        }
                        ((TextView)
                                messageSnackbar
                                        .getView()
                                        .findViewById(android.support.design.R.id.snackbar_text))
                                .setMaxLines(maxLines);
                        messageSnackbar.show();
                    }
                });
    }
}