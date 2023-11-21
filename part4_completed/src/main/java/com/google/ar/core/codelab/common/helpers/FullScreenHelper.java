package com.google.ar.core.codelab.common.helpers;

import android.app.Activity;
import android.view.View;

/** Android tam ekran modunu ayarlamak için yardımcı sınıf. */
public final class FullScreenHelper {
  /**
   * Android tam ekran bayraklarını ayarlar. {@link
   * Activity#onWindowFocusChanged(boolean hasFocus)}'den çağrılması beklenir.
   *
   * @param activity tam ekran modunun ayarlanacağı Activity.
   * @param hasFocus {@link Activity#onWindowFocusChanged(boolean hasFocus)} geri çağrısından
   *     iletilen hasFocus bayrağı.
   */
  public static void setFullScreenOnWindowFocusChanged(Activity activity, boolean hasFocus) {
    if (hasFocus) {
      // https://developer.android.com/training/system-ui/immersive.html#sticky
      activity
              .getWindow()
              .getDecorView()
              .setSystemUiVisibility(
                      View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                              | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                              | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                              | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                              | View.SYSTEM_UI_FLAG_FULLSCREEN
                              | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
  }
}