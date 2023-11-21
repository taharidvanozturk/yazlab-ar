package com.google.ar.core.codelab.common.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/** Kamera izni için yardımcı sınıf. */
public final class CameraPermissionHelper {
  private static final int CAMERA_PERMISSION_CODE = 0;
  private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;

  /** Bu uygulama için gerekli izinlere sahip olup olmadığımızı kontrol eder. */
  public static boolean hasCameraPermission(Activity activity) {
    return ContextCompat.checkSelfPermission(activity, CAMERA_PERMISSION)
            == PackageManager.PERMISSION_GRANTED;
  }

  /** Bu uygulama için gerekli izinlere sahip olup olmadığımızı kontrol eder ve yoksa talep eder. */
  public static void requestCameraPermission(Activity activity) {
    ActivityCompat.requestPermissions(
            activity, new String[] {CAMERA_PERMISSION}, CAMERA_PERMISSION_CODE);
  }

  /** Bu izin için kullanıcıya açıklama göstermemiz gerekip gerekmediğini kontrol eder. */
  public static boolean shouldShowRequestPermissionRationale(Activity activity) {
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, CAMERA_PERMISSION);
  }

  /** İzin verme ayarlarına gitmek için Uygulama Ayarlarını başlatır. */
  public static void launchPermissionSettings(Activity activity) {
    Intent intent = new Intent();
    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
    activity.startActivity(intent);
  }
}