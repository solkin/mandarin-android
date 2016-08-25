package com.tomclaw.mandarin.util;

import android.graphics.*;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.LongSparseArray;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.IntBuffer;
import java.util.concurrent.Semaphore;

public class GifDrawable extends Drawable implements Animatable {

    private static final String TAG = "GifImageView";
    private static final boolean INFO = true;
    private static final boolean DEBUG = true;
    private static final boolean VERBOSE = true;

    private Bitmap imageBitmap;

    private final Matrix matrix = new Matrix();
    private final GifFileDecoder decoder;

    public static interface DiagnosticsCallback {
        public void onDiagnostics(String value);
    }

    public static DiagnosticsCallback diagnosticsCallback;

    public static final int STATE_STOPPED = 0;
    public static final int STATE_PLAYING = 1;
    public static final int STATE_PAUSED = 2;

    protected final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);

    public GifDrawable(GifFileDecoder decoder) {
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        paint.setDither(false);

        this.decoder = decoder;
        imageBitmap = Bitmap.createBitmap(decoder.getWidth(), decoder.getHeight(),
                Bitmap.Config.ARGB_8888);
    }

    public int getGifState() {
        return GifDrawable.getState(this);
    }

    public void play() {
        switch (getGifState()) {
            case STATE_PLAYING:
                if (INFO) Log.i(TAG, "already playing");
                break;
            case STATE_STOPPED:
                GifDrawable.start(this);
                break;
            case STATE_PAUSED:
                GifDrawable.resume(this);
        }
    }

    public void pause() {
        if (getGifState() == STATE_PLAYING) {
            GifDrawable.pause(this);
        } else {
            Log.w(TAG, "can't pause");
        }
    }

    public void stop() {
        if (getGifState() == STATE_PLAYING || getGifState() == STATE_PAUSED) {
            GifDrawable.stop(this);
        } else {
            Log.w(TAG, "can't stop");
        }
    }


    // Static dispatcher methods

    private static final int MSG_REDRAW = 1;
    private static final int MSG_FINALIZE = 2;

    private static LongSparseArray<ThreadInfo> threads = new LongSparseArray<ThreadInfo>();
    private static Handler mainHandler;

    private static class ThreadInfo {
        public WeakReference<GifDrawable> view;
        public Semaphore pause = new Semaphore(1);
        public boolean paused = false;
    }

    private static class ThreadParam {
        public long threadId;
        public Bitmap bitmap;
    }

    static {
        mainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                mainThread(msg.what, (ThreadParam) msg.obj);
            }
        };
    }

    private synchronized static void start(GifDrawable view) {
        if (INFO) Log.i(TAG, "start");

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                backgroundThread();
            }
        });

        ThreadInfo info = new ThreadInfo();
        info.view = new WeakReference<>(view);
        threads.put(thread.getId(), info);

        thread.start();
    }

    private synchronized static void stop(GifDrawable view) {
        if (INFO) Log.i(TAG, "stop");
        ThreadInfo info = getThreadInfo(view);
        if (info != null) {
            stopThread(info);
        }
    }

    public synchronized static void stopAll() {
        for (int i = 0; i < threads.size(); i++) {
            stopThread(threads.valueAt(i));
        }
    }

    private static void stopThread(ThreadInfo info) {
        info.view.clear();
        if (info.paused) {
            info.pause.release();
            info.paused = false;
        }
    }

    private synchronized static void pause(GifDrawable view) {
        if (INFO) Log.i(TAG, "pause");
        ThreadInfo info = getThreadInfo(view);
        if (info != null && !info.paused) {
            try {
                info.pause.acquire();
            } catch (InterruptedException ex) {
            }
            info.paused = true;
        }
    }

    private synchronized static void resume(GifDrawable view) {
        if (INFO) Log.i(TAG, "resume");
        ThreadInfo info = getThreadInfo(view);
        if (info != null && info.paused) {
            info.pause.release();
            info.paused = false;
        }
    }

    private synchronized static int getState(GifDrawable view) {
        ThreadInfo info = getThreadInfo(view);
        if (info == null) {
            return STATE_STOPPED;
        }
        if (info.paused) {
            return STATE_PAUSED;
        }
        return STATE_PLAYING;
    }

    private synchronized static ThreadInfo getThreadInfo(GifDrawable view) {
        for (int i = 0; i < threads.size(); i++) {
            ThreadInfo info = threads.valueAt(i);
            GifDrawable threadView = info.view.get();
            if (view.equals(threadView))
                return info;
        }
        return null;
    }

    private synchronized static ThreadInfo getThreadInfo(long threadId) {
        return threads.get(threadId);
    }

    private synchronized static void removeThread(long threadId) {
        threads.remove(threadId);
    }

    private static void backgroundThread() {
        long threadId = Thread.currentThread().getId();
        ThreadInfo info = getThreadInfo(threadId);
        if (info == null) {
            return;
        }

        if (DEBUG) Log.d(TAG, "started thread " + threadId);

        long startTime = System.currentTimeMillis();
        long infoTime = startTime + 10 * 1000;
        int delay = 0;
        long frameIndex = 0;
        long decodeTimes = 0;
        long delays = 0;
        boolean diagDone = false;

        GifDrawable view = info.view.get();
        GifFileDecoder decoder;
        if (view != null) {
            decoder = view.decoder;
            Bitmap bitmap = view.imageBitmap;
            try {
                while (decoder.hasFrame()) {
                    // decode frame
                    long frameStart = System.currentTimeMillis();

                    int[] pixels = decoder.readFrame();
                    if (pixels == null) {
                        if (DEBUG) Log.d(TAG, "null frame, stopping");
                        break;
                    }

                    long decodeTime = System.currentTimeMillis() - frameStart;

                    if (VERBOSE) Log.v(TAG, "decoded frame in " + decodeTime + " delay " + delay);

                    // wait until the end of delay set by previous frame
                    Thread.sleep(Math.max(0, delay - decodeTime));

                    // check for pause
                    info.pause.acquire();
                    info.pause.release();

                    // check if view still exists
                    if (info.view.get() == null) {
                        break;
                    }

                    // send frame to view
                    bitmap.copyPixelsFromBuffer(IntBuffer.wrap(pixels));
                    sendToMain(threadId, MSG_REDRAW, null);

                    delay = decoder.getDelay();

                    // some logging
                    if (diagnosticsCallback != null && !diagDone) {
                        frameIndex++;
                        decodeTimes += decodeTime;
                        delays += delay;
                        if (System.currentTimeMillis() > startTime + 5 * 1000) {
                            long fpsa = frameIndex * 1000 / decodeTimes;
                            long fpsb = frameIndex * 1000 / delays;
                            String value = "size: " + bitmap.getWidth() + " x " + bitmap.getHeight() +
                                    "\nfps: " + fpsa + " / " + fpsb;
                            diagnosticsCallback.onDiagnostics(value);
                            diagDone = true;
                        }
                    }
                    if (System.currentTimeMillis() > infoTime) {
                        if (INFO) Log.i(TAG, "Gif thread still running");
                        infoTime += 10 * 1000;
                    }
                    if (System.currentTimeMillis() > startTime + 4 * 60 * 60 * 1000) {
                        throw new RuntimeException("Gif thread leaked, fix your code");
                    }
                }
            } catch (IOException ex) {
                Logger.log("gif drawable warn", ex);
            } catch (InterruptedException ex) {
                Logger.log("gif drawable err", ex);
            } finally {
                if (DEBUG) Log.d(TAG, "stopping decoder");
                decoder.stop();
            }
        }

        sendToMain(threadId, MSG_FINALIZE, null);
        if (DEBUG) Log.d(TAG, "finished thread " + threadId);
    }

    private static void sendToMain(long threadId, int what, Bitmap bitmap) {
        ThreadParam param = new ThreadParam();
        param.threadId = threadId;
        param.bitmap = bitmap;
        mainHandler.obtainMessage(what, param).sendToTarget();
    }

    private static void mainThread(int what, ThreadParam obj) {
        if (what == MSG_FINALIZE) {
            if (DEBUG) Log.d(TAG, "removing thread " + obj.threadId);
            removeThread(obj.threadId);
            return;
        }

        ThreadInfo info = getThreadInfo(obj.threadId);
        if (info == null) {
            if (DEBUG) Log.d(TAG, "no thread info");
            return;
        }
        GifDrawable view = info.view.get();
        if (view == null) {
            if (DEBUG) Log.d(TAG, "no view");
            return;
        }

        if (what == MSG_REDRAW) {
            view.invalidateSelf();
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        matrix.setRectToRect(new RectF(0, 0, imageBitmap.getWidth(), imageBitmap.getHeight()),
                new RectF(getBounds()), Matrix.ScaleToFit.CENTER);
    }

    @Override
    public void draw(Canvas canvas) {
        if (imageBitmap != null) {
            canvas.drawBitmap(imageBitmap, matrix, paint);
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return imageBitmap.getWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return imageBitmap.getHeight();
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    @Override
    public void start() {
        play();
    }

    @Override
    public boolean isRunning() {
        return getGifState() == STATE_PLAYING;
    }

}
