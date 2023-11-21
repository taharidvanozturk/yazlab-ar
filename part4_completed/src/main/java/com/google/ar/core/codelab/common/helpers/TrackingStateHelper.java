package com.google.ar.core.codelab.common.helpers;

import android.app.Activity;
import android.view.WindowManager;
import com.google.ar.core.Camera;
import com.google.ar.core.TrackingFailureReason;
import com.google.ar.core.TrackingState;

/** Takip başarısızlık nedenlerini ve önerilen eylemleri insan tarafından okunabilir bir şekilde alır. */
public final class TrackingStateHelper {
  private static final String INSUFFICIENT_FEATURES_MESSAGE =
          "Hiçbir şey bulunamıyor. Cihazı daha fazla dokulu veya renkli bir yüze doğrultun.";
  private static final String EXCESSIVE_MOTION_MESSAGE = "Çok hızlı hareket ediliyor. Yavaşlayın.";
  private static final String INSUFFICIENT_LIGHT_MESSAGE =
          "Çok karanlık. Aydınlık bir bölgeye taşının.";
  private static final String BAD_STATE_MESSAGE =
          "Kötü iç durum nedeniyle takip kayboldu. Lütfen AR deneyimini yeniden başlatmayı deneyin.";
  private static final String CAMERA_UNAVAILABLE_MESSAGE =
          "Başka bir uygulama kamerayı kullanıyor. Bu uygulamaya dokunun veya diğerini kapatmayı deneyin.";

  private final Activity activity;

  private TrackingState previousTrackingState;

  public TrackingStateHelper(Activity activity) {
    this.activity = activity;
  }

  /** Takip sürdüğünde ekranın kilitli kalmasını sağlar, ancak takip durduğunda kilitlenmesine izin verir. */
  public void updateKeepScreenOnFlag(TrackingState trackingState) {
    if (trackingState == previousTrackingState) {
      return;
    }

    previousTrackingState = trackingState;
    switch (trackingState) {
      case PAUSED:
      case STOPPED:
        activity.runOnUiThread(
                () -> activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        break;
      case TRACKING:
        activity.runOnUiThread(
                () -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        break;
    }
  }

  public static String getTrackingFailureReasonString(Camera camera) {
    TrackingFailureReason reason = camera.getTrackingFailureReason();
    switch (reason) {
      case NONE:
        return "";
      case BAD_STATE:
        return BAD_STATE_MESSAGE;
      case INSUFFICIENT_LIGHT:
        return INSUFFICIENT_LIGHT_MESSAGE;
      case EXCESSIVE_MOTION:
        return EXCESSIVE_MOTION_MESSAGE;
      case INSUFFICIENT_FEATURES:
        return INSUFFICIENT_FEATURES_MESSAGE;
      case CAMERA_UNAVAILABLE:
        return CAMERA_UNAVAILABLE_MESSAGE;
    }
    return "Bilinmeyen takip başarısızlık nedeni: " + reason;
  }
}