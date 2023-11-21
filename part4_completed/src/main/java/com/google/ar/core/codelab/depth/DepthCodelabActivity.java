/*
 * Telif Hakkı 2020 Google Inc. Tüm Hakları Saklıdır.
 *
 * Apache Lisansı, Sürüm 2.0 (the "License");
 * Bu dosyayı lisansın gerektirdiği şekilde kullanamazsınız.
 * Lisansın bir kopyasını şu adresten alabilirsiniz:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Uygulanabilir yasa gereği veya yazılı izin olmaksızın,
 * Lisans altındaki yazılım "OLDUĞU GİBİ" TEMELİNDE, GARANTİSİZ VEYA KOŞULLU OLARAK
 * DAĞITILIR. Belirli bir dil için belirli haklar ve sınırlamalar için lisansa bakın.
 */

package com.google.ar.core.codelab.depth;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Point.OrientationMode;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.codelab.common.helpers.CameraPermissionHelper;
import com.google.ar.core.codelab.common.helpers.DisplayRotationHelper;
import com.google.ar.core.codelab.common.helpers.FullScreenHelper;
import com.google.ar.core.codelab.common.helpers.SnackbarHelper;
import com.google.ar.core.codelab.common.helpers.TapHelper;
import com.google.ar.core.codelab.common.helpers.TrackingStateHelper;
import com.google.ar.core.codelab.common.rendering.BackgroundRenderer;
import com.google.ar.core.codelab.common.rendering.ObjectRenderer;
import com.google.ar.core.codelab.common.rendering.OcclusionObjectRenderer;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import java.io.IOException;
import java.util.ArrayList;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Bu, ARCore API'sını kullanarak artırılmış gerçeklik (AR) uygulaması oluşturan basit bir örnektir.
 * Uygulama, kullanıcının dokunarak Android robotun 3D modelini yerleştirmesine izin verir.
 */
public class DepthCodelabActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
  private static final String TAG = DepthCodelabActivity.class.getSimpleName();

  // Rendering. Renderers burada oluşturulur ve GL yüzeyi oluşturulduğunda başlatılır.
  private GLSurfaceView surfaceView;

  private boolean installRequested;
  private boolean isDepthSupported;

  private Session session;
  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private DisplayRotationHelper displayRotationHelper;
  private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
  private TapHelper tapHelper;

  private final DepthTextureHandler depthTexture = new DepthTextureHandler();
  private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
  private final ObjectRenderer virtualObject = new ObjectRenderer();
  private final OcclusionObjectRenderer occludedVirtualObject = new OcclusionObjectRenderer();

  // Geçici matris, her bir çerçeve için yapılan işlemleri azaltmak için burada ayrılmıştır.
  private final float[] anchorMatrix = new float[16];

  private static final String SEARCHING_PLANE_MESSAGE = "Lütfen yavaşça etrafta dolaşın...";
  private static final String PLANES_FOUND_MESSAGE = "Nesneleri yerleştirmek için dokunun.";
  private static final String DEPTH_NOT_AVAILABLE_MESSAGE = "[Bu cihazda derinlik desteklenmiyor]";

  // Dokunarak oluşturulan nesneler için renkli bir şekilde kullanılan Anchor'lar.
  private static final float[] OBJECT_COLOR = new float[] {139.0f, 195.0f, 74.0f, 255.0f};
  private final ArrayList<Anchor> anchors = new ArrayList<>();

  private boolean showDepthMap = true;
  private boolean calculateUVTransform = true;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    surfaceView = findViewById(R.id.surfaceview);
    displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

    // Dokunma dinleyicisini kur.
    tapHelper = new TapHelper(/*context=*/ this);
    surfaceView.setOnTouchListener(tapHelper);

    // Renderer'ı kur.
    surfaceView.setPreserveEGLContextOnPause(true);
    surfaceView.setEGLContextClientVersion(2);
    surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha, düzlem karıştırma için kullanılır.
    surfaceView.setRenderer(this);
    surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    surfaceView.setWillNotDraw(false);

    installRequested = false;

    final Button toggleDepthButton = (Button) findViewById(R.id.toggle_depth_button);
    toggleDepthButton.setOnClickListener(
            view -> {
              if (isDepthSupported) {
                showDepthMap = !showDepthMap;
                toggleDepthButton.setText(showDepthMap ? R.string.hide_depth : R.string.show_depth);
              } else {
                showDepthMap = false;
                toggleDepthButton.setText(R.string.depth_not_available);
              }
            });
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (session == null) {
      Exception exception = null;
      String message = null;
      try {
        switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
          case INSTALL_REQUESTED:
            installRequested = true;
            return;
          case INSTALLED:
            break;
        }

        // ARCore'un çalışması için kamera izinlerine ihtiyaç duyar. Android M ve üzerinde
        // henüz çalışma zamanı izni almadıysak, şimdi kullanıcıdan istemek iyi bir zamandır.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
          CameraPermissionHelper.requestCameraPermission(this);
          return;
        }

        // ARCore session oluştur.
        session = new Session(/* context= */ this);
        Config config = session.getConfig();
        isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
        if (isDepthSupported) {
          config.setDepthMode(Config.DepthMode.AUTOMATIC);
        } else {
          config.setDepthMode(Config.DepthMode.DISABLED);
        }
        session.configure(config);


      } catch (UnavailableArcoreNotInstalledException
               | UnavailableUserDeclinedInstallationException e) {
        message = "Lütfen ARCore'u yükleyin";
        exception = e;
      } catch (UnavailableApkTooOldException e) {
        message = "Lütfen ARCore'u güncelleyin";
        exception = e;
      } catch (UnavailableSdkTooOldException e) {
        message = "Lütfen bu uygulamayı güncelleyin";
        exception = e;
      } catch (UnavailableDeviceNotCompatibleException e) {
        message = "Bu cihaz AR'yi desteklemiyor";
        exception = e;
      } catch (Exception e) {
        message = "AR session oluşturma başarısız oldu";
        exception = e;
      }

      if (message != null) {
        messageSnackbarHelper.showError(this, message);
        Log.e(TAG, "Session oluşturulurken hata oluştu", exception);
        return;
      }
    }

    // Not: Sıralamanın önemi vardır - onPause() içindeki notu görmek için, tersi burada geçerlidir.
    try {
      session.resume();
    } catch (CameraNotAvailableException e) {
      messageSnackbarHelper.showError(this, "Kamera kullanılamıyor. Uygulamayı yeniden başlatmayı deneyin.");
      session = null;
      return;
    }

    surfaceView.onResume();
    displayRotationHelper.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    if (session != null) {
      // Not: Sıralama önemlidir - GLSurfaceView önce durdurulur, böylece session'ı sorgulamaz.
      // Eğer Session, GLSurfaceView'den önce durdurulursa, GLSurfaceView hala session.update() çağırabilir
      // ve SessionPausedException alabilir.
      displayRotationHelper.onPause();
      surfaceView.onPause();
      session.pause();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      Toast.makeText(this, "Bu uygulamayı çalıştırmak için kamera izni gereklidir",
              Toast.LENGTH_LONG).show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // "Tekrar Sorma" kontrol edilmişse izin reddedildi.
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

    // Rendering nesnelerini hazırla. Bu, shader'ları okuma içerir, bu nedenle IOException fırlatabilir.
    try {
      // Derinlik dokusu, nesne gizliliği ve render için kullanılır.
      depthTexture.createOnGlThread();

      // Texture oluştur ve ARCore session'a geç, update() sırasında doldurulması için.
      backgroundRenderer.createOnGlThread(/*context=*/ this);
      backgroundRenderer.createDepthShaders(/*context=*/ this, depthTexture.getDepthTexture());

      virtualObject.createOnGlThread(/*context=*/ this, "models/andy.obj", "models/andy.png");
      virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

      if (isDepthSupported) {
        occludedVirtualObject.createOnGlThread(/*context=*/ this, "models/andy.obj", "models/andy.png");
        occludedVirtualObject.setDepthTexture(
                depthTexture.getDepthTexture(),
                depthTexture.getDepthWidth(),
                depthTexture.getDepthHeight());
        occludedVirtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);
      }
    } catch (IOException e) {
      Log.e(TAG, "Asset dosyası okuma başarısız oldu", e);
    }
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    displayRotationHelper.onSurfaceChanged(width, height);
    GLES20.glViewport(0, 0, width, height);
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    // Önceki çerçeveden herhangi bir pikselin yüklenmemesi için ekranı temizle.
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

    if (session == null) {
      return;
    }
    // ARCore session'ına görünüm boyutunun değiştiğini bildir, bu nedenle perspektif matrisi ve
    // video arka planı uygun şekilde ayarlanabilir.
    displayRotationHelper.updateSessionIfNeeded(session);

    try {
      session.setCameraTextureName(backgroundRenderer.getTextureId());

      // ARSession'dan güncel çerçeveyi al. Konfigürasyon UpdateMode.BLOCKING olarak ayarlandığında
      // (varsayılan olarak), bu, render'ı kameranın kare hızına ayarlar.
      Frame frame = session.update();
      Camera camera = frame.getCamera();

      if (frame.hasDisplayGeometryChanged() || calculateUVTransform) {
        calculateUVTransform = false;
        float[] transform = getTextureTransformMatrix(frame);
        occludedVirtualObject.setUvTransformMatrix(transform);
      }
// Bu çerçeve için en son derinlik görüntüsünü alır.
      if (isDepthSupported) {
        depthTexture.update(frame);
      }

// Her karede bir dokunma işlemini ele alır.
      handleTap(frame, camera);

// Eğer çerçeve hazırsa, kamera önizleme görüntüsünü GL yüzeyine çizer.
      backgroundRenderer.draw(frame);
      if (showDepthMap) {
        backgroundRenderer.drawDepth(frame);
      }

// Ekrana dokunulduğunda ekranın kilidini açık tut, ancak takip durduğunda kilitlemeye izin ver.
      trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

// Takip yapılmıyorsa, 3D nesneleri çizme; takip hatası durumunu göster.
      if (camera.getTrackingState() == TrackingState.PAUSED) {
        messageSnackbarHelper.showMessage(
                this, TrackingStateHelper.getTrackingFailureReasonString(camera));
        return;
      }

// Projeksiyon matrisini al.
      float[] projmtx = new float[16];
      camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

// Kamera matrisini al ve çiz.
      float[] viewmtx = new float[16];
      camera.getViewMatrix(viewmtx, 0);

// Görüntünün ortalama yoğunluğundan aydınlatmayı hesapla.
// İlk üç bileşen renk ölçekleme faktörleridir.
// Sonuncusu gamma uzayındaki ortalama piksel yoğunluğudur.
      final float[] colorCorrectionRgba = new float[4];
      frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

// Bu noktada takip hatası yok. Uçaklar bulunup bulunmadığına bağlı olarak kullanıcıya ne yapılacağını bildirin.
      String messageToShow = "";
      if (hasTrackingPlane()) {
        messageToShow = PLANES_FOUND_MESSAGE;
      } else {
        messageToShow = SEARCHING_PLANE_MESSAGE;
      }
      if (!isDepthSupported) {
        messageToShow += "\n" +  DEPTH_NOT_AVAILABLE_MESSAGE;
      }
      messageSnackbarHelper.showMessage(this, messageToShow);

// Dokunma ile oluşturulan anchor'ları görselleştir.
      float scaleFactor = 1.0f;
      for (Anchor anchor : anchors) {
        if (anchor.getTrackingState() != TrackingState.TRACKING) {
          continue;
        }
        // Bir Anchor'ın dünya uzayındaki mevcut durumunu al. Anchor pozisyonu
        // ARCore'un dünya tahminini iyileştirdikçe güncellenir.
        anchor.getPose().toMatrix(anchorMatrix, 0);

        // Model ve gölgesini güncelle ve çiz.
        if (isDepthSupported) {
          occludedVirtualObject.updateModelMatrix(anchorMatrix, scaleFactor);
          occludedVirtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, OBJECT_COLOR);
        } else {
          virtualObject.updateModelMatrix(anchorMatrix, scaleFactor);
          virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, OBJECT_COLOR);
        }
      }

    } catch (Throwable t) {
// İstisnasız durumlar nedeniyle uygulamanın çökmesini önleyin.
      Log.e(TAG, "OpenGL thread üzerinde istisna", t);
    }
  }

  // Yalnızca bir dokunma işlemi işle, çünkü dokunmalar genellikle kare hızına göre düşük frekanstır.
  private void handleTap(Frame frame, Camera camera) {
    MotionEvent tap = tapHelper.poll();
    if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
      for (HitResult hit : frame.hitTest(tap)) {
        // Herhangi bir uçağın vurulup vurulmadığını ve vurulan yerin uçak çokgeni içinde olup olmadığını kontrol edin.
        Trackable trackable = hit.getTrackable();
        // Bir uçağa veya yönlendirilmiş bir noktaya vurulduysa bir anchor oluşturun.
        if ((trackable instanceof Plane
                && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                && (calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
                || (trackable instanceof Point
                && ((Point) trackable).getOrientationMode()
                == OrientationMode.ESTIMATED_SURFACE_NORMAL)) {
          // Vurulanları derinliğine göre sırala. Sadece uçağa veya yönlendirilmiş bir noktaya en yakın vuruşu düşünün.
          // Oluşturulan nesnelerin sayısını sınırlayın. Bu, hem
          // rendering sistemi hem de ARCore'u aşırı yüklemekten kaçınır.
          if (anchors.size() >= 20) {
            anchors.get(0).detach();
            anchors.remove(0);
          }

          // Bir Anchor eklemek, ARCore'un bu konumu
          // uzayda takip etmesi gerektiğini belirtir. Bu anchor, 3D modeli
          // hem dünya hem de uçağa göre doğru konumlandırmak için Plane üzerinde oluşturulur.
          anchors.add(hit.createAnchor());
          break;
        }
      }
    }
  }

  // En az bir uçağın algılanıp algılanmadığını kontrol eder.
  private boolean hasTrackingPlane() {
    for (Plane plane : session.getAllTrackables(Plane.class)) {
      if (plane.getTrackingState() == TrackingState.TRACKING) {
        return true;
      }
    }
    return false;
  }

  // cameraPose'dan planePose'a kadar olan düzleme normal mesafeyi hesaplar, verilen planePose'un y ekseni
// düzlemin normaliyle paralel olmalıdır, örneğin düzlemin merkezi durumu veya vuruş testi durumu.
  private static float calculateDistanceToPlane(Pose planePose, Pose cameraPose) {
    float[] normal = new float[3];
    float cameraX = cameraPose.tx();
    float cameraY = cameraPose.ty();
    float cameraZ = cameraPose.tz();
// Düzlemin koordinat sisteminin dönüştürülmüş Y eksenini alın.
    planePose.getTransformedAxis(1, 1.0f, normal, 0);
// Düzlemin normali ile kameradan düzleme merkezine giden vektörün iç çarpımını hesaplayın.
    return (cameraX - planePose.tx()) * normal[0]
            + (cameraY - planePose.ty()) * normal[1]
            + (cameraZ - planePose.tz()) * normal[2];
  }

  /**
   * Bu yöntem, ekran uzayındaki uvs'yi doğru bir şekilde eşleştirmek için kullanılan bir dönüşüm matrisi döndürür.
   * Cihazın yönelimini dikkate alır.
   */
  private static float[] getTextureTransformMatrix(Frame frame) {
    float[] frameTransform = new float[6];
    float[] uvTransform = new float[9];
// NDC uzayındaki orijin ve iki ana eksenden oluşan koordinat çiftleri.
    float[] ndcBasis = {0, 0, 1, 0, 0, 1};

// Geçici olarak dönüştürülmüş noktaları outputTransform içinde depolayın.
    frame.transformCoordinates2d(
            Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
            ndcBasis,
            Coordinates2d.TEXTURE_NORMALIZED,
            frameTransform);

// Dönüştürülmüş noktaları bir afin dönüşüme çevirin ve transpoze edin.
    float ndcOriginX = frameTransform[0];
    float ndcOriginY = frameTransform[1];
    uvTransform[0] = frameTransform[2] - ndcOriginX;
    uvTransform[1] = frameTransform[3] - ndcOriginY;
    uvTransform[2] = 0;
    uvTransform[3] = frameTransform[4] - ndcOriginX;
    uvTransform[4] = frameTransform[5] - ndcOriginY;
    uvTransform[5] = 0;
    uvTransform[6] = ndcOriginX;
    uvTransform[7] = ndcOriginY;
    uvTransform[8] = 1;

    return uvTransform;
  }
}