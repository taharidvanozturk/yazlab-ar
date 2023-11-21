package com.google.ar.core.codelab.depth;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES30.GL_LINEAR;
import static android.opengl.GLES30.GL_RG;
import static android.opengl.GLES30.GL_RG8;

import android.media.Image;
import com.google.ar.core.Frame;
import com.google.ar.core.exceptions.NotYetAvailableException;

/** DEPTH16 derinlik görüntüsünü içeren RG8 GPU dokusunu işler. */
public final class DepthTextureHandler {

  private int depthTextureId = -1;
  private int depthTextureWidth = -1;
  private int depthTextureHeight = -1;

  /**
   * Derinlik dokusunu oluşturur ve başlatır. Bu yöntem, bir EGL bağlamına sahip bir iş parçacığı üzerinde çağrılmalıdır.
   */
  public void createOnGlThread() {
    int[] textureId = new int[1];
    glGenTextures(1, textureId, 0);
    depthTextureId = textureId[0];
    glBindTexture(GL_TEXTURE_2D, depthTextureId);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
  }

  /**
   * Derinlik dokusunu, acquireDepthImage() içeriği ile günceller.
   * Bu yöntem, bir EGL bağlamına sahip bir iş parçacığı üzerinde çağrılmalıdır.
   */
  public void update(final Frame frame) {
    try {
      Image depthImage = frame.acquireDepthImage16Bits();
      depthTextureWidth = depthImage.getWidth();
      depthTextureHeight = depthImage.getHeight();
      glBindTexture(GL_TEXTURE_2D, depthTextureId);
      glTexImage2D(
              GL_TEXTURE_2D,
              0,
              GL_RG8,
              depthTextureWidth,
              depthTextureHeight,
              0,
              GL_RG,
              GL_UNSIGNED_BYTE,
              depthImage.getPlanes()[0].getBuffer());
      depthImage.close();
    } catch (NotYetAvailableException e) {
      // Bu genellikle derinlik verilerinin henüz mevcut olmadığı anlamına gelir.
    }
  }

  public int getDepthTexture() {
    return depthTextureId;
  }

  public int getDepthWidth() {
    return depthTextureWidth;
  }

  public int getDepthHeight() {
    return depthTextureHeight;
  }
}