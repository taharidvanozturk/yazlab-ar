package com.google.ar.core.codelab.common.helpers;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import com.google.ar.core.Session;

/**
 * Ekran rotasyonlarını takip etmek için yardımcı sınıf. Özellikle, 180 derece rotasyonları,
 * onSurfaceChanged() geri çağrısı tarafından bildirilmez ve bu nedenle android ekran
 * olaylarını dinlemeyi gerektirir.
 */
public final class DisplayRotationHelper implements DisplayListener {
  private boolean viewportChanged;
  private int viewportWidth;
  private int viewportHeight;
  private final Display display;
  private final DisplayManager displayManager;
  private final CameraManager cameraManager;

  /**
   * DisplayRotationHelper'ı oluşturur ancak henüz dinleyiciyi kaydetmez.
   *
   * @param context Android {@link Context}.
   */
  public DisplayRotationHelper(Context context) {
    displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
    cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    display = windowManager.getDefaultDisplay();
  }

  /** Display dinleyicisini kaydeder.*/
  public void onResume() {
    displayManager.registerDisplayListener(this, null);
  }

  /** Display dinleyicisini kaydeder.*/
  public void onPause() {
    displayManager.unregisterDisplayListener(this);
  }

  /**
   * Yüzey boyutundaki bir değişikliği kaydeder. Bu daha sonra {@link #updateSessionIfNeeded(Session)} çağrısı
   * veya {@link #onDisplayChanged(int)} sistem geri çağrısı tarafından kullanılacaktır.
   * {@link android.opengl.GLSurfaceView.Renderer
   * #onSurfaceChanged(javax.microedition.khronos.opengles.GL10, int, int)} içinden çağrılmalıdır.
   *
   * @param width yüzeyin güncellenmiş genişliği.
   * @param height yüzeyin güncellenmiş yüksekliği.
   */
  public void onSurfaceChanged(int width, int height) {
    viewportWidth = width;
    viewportHeight = height;
    viewportChanged = true;
  }

  /**
   * Eğer bir değişiklik postalandıysa, bu fonksiyon çağrılacak şekilde günceller
   * session ekran geometrisini {@link Session#update()} çağrısından önce. Bu
   * fonksiyon aynı zamanda 'bekleyen güncelleme' (viewportChanged) bayrağını da temizler.
   *
   * @param session ekran geometrisi değiştiyse güncellenecek {@link Session} nesnesi.
   */
  public void updateSessionIfNeeded(Session session) {
    if (viewportChanged) {
      int displayRotation = display.getRotation();
      session.setDisplayGeometry(displayRotation, viewportWidth, viewportHeight);
      viewportChanged = false;
    }
  }

  /**
   *  GL yüzey viewport'unun ekran rotasyonunu dikkate alarak hesaplanmış en-boy oranını döndürür.
   */
  public float getCameraSensorRelativeViewportAspectRatio(String cameraId) {
    float aspectRatio;
    int cameraSensorToDisplayRotation = getCameraSensorToDisplayRotation(cameraId);
    switch (cameraSensorToDisplayRotation) {
      case 90:
      case 270:
        aspectRatio = (float) viewportHeight / (float) viewportWidth;
        break;
      case 0:
      case 180:
        aspectRatio = (float) viewportWidth / (float) viewportHeight;
        break;
      default:
        throw new RuntimeException("İşlenmemiş rotasyon: " + cameraSensorToDisplayRotation);
    }
    return aspectRatio;
  }

  /**
   * Belirtilen kamera için ekranla kamera sensörü arasındaki rotasyonu döndürür.
   * Değerler 0, 90, 180, 270 olabilir.
   */
  public int getCameraSensorToDisplayRotation(String cameraId) {
    CameraCharacteristics characteristics;
    try {
      characteristics = cameraManager.getCameraCharacteristics(cameraId);
    } catch (CameraAccessException e) {
      throw new RuntimeException("Ekran yönetimini belirleme başarısız", e);
    }

    // Kamera sensörü orientasyonu.
    int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

    // Geçerli ekran orientasyonu.
    int displayOrientation = toDegrees(display.getRotation());

    // 0, 90, 180 veya 270 derece döndürdüğümüzden emin olun.
    return (sensorOrientation - displayOrientation + 360) % 360;
  }

  private int toDegrees(int rotation) {
    switch (rotation) {
      case Surface.ROTATION_0:
        return 0;
      case Surface.ROTATION_90:
        return 90;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_270:
        return 270;
      default:
        throw new RuntimeException("Bilinmeyen rotasyon " + rotation);
    }
  }

  @Override
  public void onDisplayAdded(int displayId) {}

  @Override
  public void onDisplayRemoved(int displayId) {}

  @Override
  public void onDisplayChanged(int displayId) {
    viewportChanged = true;
  }
}