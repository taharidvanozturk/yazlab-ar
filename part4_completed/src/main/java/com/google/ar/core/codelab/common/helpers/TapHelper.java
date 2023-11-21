package com.google.ar.core.codelab.common.helpers;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Android GestureDetector kullanarak dokunmaları algılamak ve dokunmaları UI thread ve
 * render thread arasında iletmek için yardımcı sınıf.
 */
public final class TapHelper implements OnTouchListener {
    private final GestureDetector gestureDetector;
    private final BlockingQueue<MotionEvent> queuedSingleTaps = new ArrayBlockingQueue<>(16);

    /**
     * TapHelper'ı oluşturur.
     *
     * @param context uygulamanın bağlamı.
     */
    public TapHelper(Context context) {
        gestureDetector =
                new GestureDetector(
                        context,
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onSingleTapUp(MotionEvent e) {
                                // Eğer sıra varsa dokunmayı sıraya ekleyin. Kuyruk doluysa dokunma kaybolur.
                                queuedSingleTaps.offer(e);
                                return true;
                            }

                            @Override
                            public boolean onDown(MotionEvent e) {
                                return true;
                            }
                        });
    }

    /**
     * Bir dokunma için sorgulama yapar.
     *
     * @return bir dokunma sıraya alındıysa dokunma için bir MotionEvent. Aksi takdirde sıra boşsa null.
     */
    public MotionEvent poll() {
        return queuedSingleTaps.poll();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return gestureDetector.onTouchEvent(motionEvent);
    }
}