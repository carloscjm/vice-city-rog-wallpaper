package com.vicecityrog.wallpaper;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * Vice City ROG v3 - renderizacao exclusivamente por Canvas.
 * 
 * Na V1/V2 o MediaPlayer e o Canvas disputavam a mesma Surface do wallpaper.
 * Aqui o video foi previamente convertido em WebP animado (12 FPS) e o
 * Android ImageDecoder/AnimatedImageDrawable cuida da decodificacao dos frames.
 * 
 * O wallpaper so anima na previa ou com a tela ACESA e BLOQUEADA.
 * Quando desbloqueado, apresenta o JPG estatico; quando invisivel, para tudo.
 */
public class ViceCityWallpaperService extends WallpaperService {
    private static final String TAG = "ViceCityROG";

    @Override public Engine onCreateEngine() {
        Log.i(TAG, "V3: criando engine Canvas/WebP");
        return new ViceCityEngine();
    }

    private final class ViceCityEngine extends Engine {
        private static final int MODE_NONE = 0;
        private static final int MODE_STATIC = 1;
        private static final int MODE_ANIMATED = 2;

        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final KeyguardManager keyguard =
                (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        private final PowerManager power =
                (PowerManager) getSystemService(Context.POWER_SERVICE);

        private boolean visible;
        private boolean surfaceReady;
        private boolean destroyed;
        private boolean receiverRegistered;
        private boolean frameScheduled;
        private int mode = MODE_NONE;
        private long lastDrawUptime;
        private Bitmap home;
        private AnimatedImageDrawable animation;

        private final Runnable updateTask = this::updateMode;
        private final Runnable frameTask = () -> {
            frameScheduled = false;
            drawAnimatedFrame();
        };
        private final Runnable animationWatchdog = new Runnable() {
            @Override public void run() {
                if (destroyed || !visible || mode != MODE_ANIMATED) return;
                if (SystemClock.uptimeMillis() - lastDrawUptime > 200) requestFrame();
                handler.postDelayed(this, 250);
            }
        };

        private final Drawable.Callback animationCallback = new Drawable.Callback() {
            @Override public void invalidateDrawable(Drawable who) {
                if (who == animation) requestFrame();
            }
            @Override public void scheduleDrawable(Drawable who, Runnable what, long when) {
                if (who == animation && !destroyed) handler.postAtTime(what, when);
            }
            @Override public void unscheduleDrawable(Drawable who, Runnable what) {
                handler.removeCallbacks(what);
            }
        };

        private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "V3: evento de tela: " + intent.getAction());
                scheduleUpdate(130);
            }
        };

        @Override public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setTouchEventsEnabled(false);
            try {
                home = BitmapFactory.decodeResource(getResources(), R.drawable.home);
                Log.i(TAG, "V3: foto carregada=" + (home != null));
            } catch (Exception e) {
                Log.e(TAG, "V3: falha abrindo JPG", e);
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            filter.addAction(Intent.ACTION_USER_UNLOCKED);
            try {
                registerReceiver(screenReceiver, filter);
                receiverRegistered = true;
            } catch (Exception e) {
                Log.e(TAG, "V3: falha registrando eventos", e);
            }
            Log.i(TAG, "V3: engine criada; preview=" + isPreview());
        }

        @Override public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            surfaceReady = true;
            mode = MODE_NONE;
            scheduleUpdate(100);
        }

        @Override public void onSurfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            super.onSurfaceChanged(holder, format, w, h);
            surfaceReady = true;
            Log.i(TAG, "V3: superficie " + w + "x" + h);
            mode = MODE_NONE;
            scheduleUpdate(140);
        }

        @Override public void onSurfaceDestroyed(SurfaceHolder holder) {
            surfaceReady = false;
            stopAnimation();
            mode = MODE_NONE;
            handler.removeCallbacks(updateTask);
            super.onSurfaceDestroyed(holder);
        }

        @Override public void onVisibilityChanged(boolean isVisible) {
            visible = isVisible;
            Log.i(TAG, "V3: visibilidade=" + visible + ", preview=" + isPreview());
            if (!visible) {
                stopAnimation();
                handler.removeCallbacks(updateTask);
                mode = MODE_NONE;
            } else {
                scheduleUpdate(160);
            }
        }

        private void scheduleUpdate(long delay) {
            handler.removeCallbacks(updateTask);
            if (!destroyed) handler.postDelayed(updateTask, delay);
        }

        private boolean canDraw() {
            return !destroyed && visible && surfaceReady &&
                    getSurfaceHolder().getSurface() != null &&
                    getSurfaceHolder().getSurface().isValid();
        }

        private boolean shouldAnimate() {
            if (isPreview()) return true;
            return keyguard != null && keyguard.isKeyguardLocked() &&
                    power != null && power.isInteractive();
        }

        private void updateMode() {
            if (!canDraw()) {
                stopAnimation();
                return;
            }
            boolean animated = shouldAnimate();
            Log.i(TAG, "V3: modo=" + (animated ? "ANIMADO" : "ESTATICO") +
                    " bloqueado=" + (keyguard != null && keyguard.isKeyguardLocked()) +
                    " telaAcesa=" + (power != null && power.isInteractive()));
            if (animated) {
                if (mode != MODE_ANIMATED) {
                    mode = MODE_ANIMATED;
                    startAnimation();
                }
            } else {
                stopAnimation();
                mode = MODE_STATIC;
                drawStaticFrame();
            }
        }

        private void startAnimation() {
            try {
                if (animation == null) {
                    Drawable decoded = ImageDecoder.decodeDrawable(
                            ImageDecoder.createSource(getResources(), R.raw.vice_city_lock_anim));
                    if (!(decoded instanceof AnimatedImageDrawable)) {
                        throw new IllegalStateException("Arquivo WebP sem animacao");
                    }
                    animation = (AnimatedImageDrawable) decoded;
                    animation.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
                    animation.setCallback(animationCallback);
                    Log.i(TAG, "V3: WebP animado decodificado: " +
                            animation.getIntrinsicWidth() + "x" + animation.getIntrinsicHeight());
                }
                animation.setCallback(animationCallback);
                animation.start();
                requestFrame();
                handler.removeCallbacks(animationWatchdog);
                handler.postDelayed(animationWatchdog, 250);
                Log.i(TAG, "V3: ANIMACAO INICIADA");
            } catch (Exception e) {
                Log.e(TAG, "V3: erro carregando WebP animado", e);
                stopAnimation();
                mode = MODE_STATIC;
                drawStaticFrame();
            }
        }

        private void requestFrame() {
            if (!frameScheduled && mode == MODE_ANIMATED && canDraw()) {
                frameScheduled = true;
                handler.post(frameTask);
            }
        }

        private void drawAnimatedFrame() {
            if (!canDraw() || mode != MODE_ANIMATED || animation == null) return;
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas == null) return;
                canvas.drawColor(Color.BLACK, PorterDuff.Mode.SRC);
                int w = Math.max(1, animation.getIntrinsicWidth());
                int h = Math.max(1, animation.getIntrinsicHeight());
                float scale = Math.max((float) canvas.getWidth() / w,
                        (float) canvas.getHeight() / h);
                float dx = (canvas.getWidth() - w * scale) * 0.5f;
                float dy = (canvas.getHeight() - h * scale) * 0.5f;
                canvas.save();
                canvas.translate(dx, dy);
                canvas.scale(scale, scale);
                animation.setBounds(0, 0, w, h);
                animation.draw(canvas);
                canvas.restore();
                lastDrawUptime = SystemClock.uptimeMillis();
            } catch (Exception e) {
                Log.e(TAG, "V3: falha desenhando frame WebP", e);
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas); }
                    catch (Exception e) { Log.e(TAG, "V3: falha enviando frame", e); }
                }
            }
        }

        private void stopAnimation() {
            handler.removeCallbacks(frameTask);
            handler.removeCallbacks(animationWatchdog);
            frameScheduled = false;
            if (animation != null && animation.isRunning()) {
                try { animation.stop(); } catch (Exception e) {
                    Log.w(TAG, "V3: erro pausando animacao", e);
                }
            }
            if (animation != null) animation.setCallback(null);
        }

        private void drawStaticFrame() {
            if (!canDraw() || home == null) return;
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas == null) return;
                canvas.drawColor(0xFF160926);
                float imageRatio = (float) home.getWidth() / home.getHeight();
                float canvasRatio = (float) canvas.getWidth() / canvas.getHeight();
                Rect src;
                if (imageRatio > canvasRatio) {
                    int cropW = Math.round(home.getHeight() * canvasRatio);
                    int left = (home.getWidth() - cropW) / 2;
                    src = new Rect(left, 0, left + cropW, home.getHeight());
                } else {
                    int cropH = Math.round(home.getWidth() / canvasRatio);
                    int top = (home.getHeight() - cropH) / 2;
                    src = new Rect(0, top, home.getWidth(), top + cropH);
                }
                canvas.drawBitmap(home, src,
                        new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), bitmapPaint);
            } catch (Exception e) {
                Log.e(TAG, "V3: falha na imagem estatica", e);
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas); }
                    catch (Exception e) { Log.e(TAG, "V3: falha enviando imagem", e); }
                }
            }
        }

        @Override public void onDestroy() {
            destroyed = true;
            stopAnimation();
            handler.removeCallbacksAndMessages(null);
            if (receiverRegistered) {
                try { unregisterReceiver(screenReceiver); }
                catch (Exception ignored) { }
            }
            if (home != null) { home.recycle(); home = null; }
            animation = null;
            super.onDestroy();
        }
    }
}
