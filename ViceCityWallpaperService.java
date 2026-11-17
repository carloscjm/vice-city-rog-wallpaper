package com.vicecityrog.wallpaper;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

/**
 * Vice City ROG v2 — Android 13, sem root.
 * - Mostra vídeo na prévia e enquanto o bloqueio estiver ativo.
 * - Mostra imagem estática na home.
 * - Nunca renderiza via Canvas enquanto o MediaPlayer usa a superfície.
 * - Registra mensagens de diagnóstico com a tag ViceCityROG.
 *
 * O firmware ASUS pode impor limitações à aplicação separada na lockscreen.
 */
public class ViceCityWallpaperService extends WallpaperService {
    private static final String TAG = "ViceCityROG";

    @Override
    public Engine onCreateEngine() {
        Log.i(TAG, "Criando novo wallpaper engine");
        return new ViceCityEngine();
    }

    private final class ViceCityEngine extends Engine {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final KeyguardManager keyguard =
                (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

        private final Runnable updateTask = this::updateRendering;
        private MediaPlayer player;
        private Bitmap image;
        private boolean visible;
        private boolean surfaceReady;
        private boolean destroyed;
        private boolean receiverRegistered;
        private boolean videoFailedThisSession;
        private int surfaceGeneration;

        private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                String action = intent == null ? "null" : intent.getAction();
                Log.d(TAG, "Broadcast: " + action);
                scheduleUpdate(160);
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setTouchEventsEnabled(false);
            image = BitmapFactory.decodeResource(getResources(), R.drawable.home);
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            filter.addAction(Intent.ACTION_USER_UNLOCKED);
            try {
                registerReceiver(screenReceiver, filter);
                receiverRegistered = true;
            } catch (Exception e) {
                Log.e(TAG, "Falha ao registrar eventos de bloqueio", e);
            }
            Log.i(TAG, "Engine iniciada (preview=" + isPreview() + ")");
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            surfaceReady = true;
            surfaceGeneration++;
            videoFailedThisSession = false;
            Log.i(TAG, "Surface criada, geracao=" + surfaceGeneration);
            scheduleUpdate(180);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            surfaceReady = true;
            surfaceGeneration++;
            videoFailedThisSession = false;
            stopVideo();
            Log.i(TAG, "Surface mudou: " + width + "x" + height
                    + ", geracao=" + surfaceGeneration);
            scheduleUpdate(200);
        }

        @Override
        public void onVisibilityChanged(boolean isVisible) {
            visible = isVisible;
            Log.i(TAG, "Visibilidade=" + visible + ", preview=" + isPreview());
            if (!visible) {
                handler.removeCallbacks(updateTask);
                stopVideo();
                videoFailedThisSession = false;
            } else {
                scheduleUpdate(150);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            Log.i(TAG, "Surface destruida");
            surfaceReady = false;
            surfaceGeneration++;
            handler.removeCallbacks(updateTask);
            stopVideo();
            super.onSurfaceDestroyed(holder);
        }

        private void scheduleUpdate(long delayMs) {
            handler.removeCallbacks(updateTask);
            if (!destroyed) handler.postDelayed(updateTask, delayMs);
        }

        private boolean hasValidSurface() {
            if (!surfaceReady) return false;
            Surface surface = getSurfaceHolder().getSurface();
            return surface != null && surface.isValid();
        }

        private boolean shouldAnimate() {
            return isPreview() || (keyguard != null && keyguard.isKeyguardLocked());
        }

        private void updateRendering() {
            if (destroyed || !visible) {
                stopVideo();
                return;
            }
            if (!hasValidSurface()) {
                Log.w(TAG, "Surface ainda nao valida; esperando");
                stopVideo();
                scheduleUpdate(350);
                return;
            }
            boolean animated = shouldAnimate();
            Log.d(TAG, "Render: animado=" + animated + ", preview=" + isPreview()
                    + ", bloqueado=" + (keyguard != null && keyguard.isKeyguardLocked())
                    + ", falhaVideo=" + videoFailedThisSession);
            if (animated) {
                if (player == null && !videoFailedThisSession) startVideo();
                else if (videoFailedThisSession) drawHome();
            } else {
                stopVideo();
                drawHome();
            }
        }

        private void startVideo() {
            if (player != null || !hasValidSurface()) return;
            final int generation = surfaceGeneration;
            final Surface surface = getSurfaceHolder().getSurface();
            final MediaPlayer mp = new MediaPlayer();
            player = mp;
            Log.i(TAG, "Abrindo video, geracao=" + generation);
            try {
                AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.vice_city_lock);
                if (afd == null) throw new IllegalStateException("MP4 compactado no APK; raw FD indisponivel");
                try {
                    mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                } finally {
                    afd.close();
                }
                mp.setSurface(surface);
                mp.setLooping(true);
                mp.setVolume(0f, 0f);

                mp.setOnPreparedListener(prepared -> {
                    if (mp != player || generation != surfaceGeneration || !visible
                            || !hasValidSurface() || !shouldAnimate()) {
                        Log.w(TAG, "Video preparado, mas surface/estado mudou");
                        if (mp == player) {
                            stopVideo();
                            scheduleUpdate(150);
                        }
                        return;
                    }
                    try {
                        prepared.setVideoScalingMode(
                                MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                        prepared.start();
                        Log.i(TAG, "VIDEO INICIADO: " + prepared.getVideoWidth()
                                + "x" + prepared.getVideoHeight());
                    } catch (Exception e) {
                        Log.e(TAG, "Falha ao iniciar video", e);
                        handleVideoFailure(mp);
                    }
                });

                mp.setOnInfoListener((active, what, extra) -> {
                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        Log.i(TAG, "PRIMEIRO FRAME RENDERIZADO");
                    }
                    return false;
                });

                mp.setOnErrorListener((failed, what, extra) -> {
                    Log.e(TAG, "ERRO MEDIAPLAYER: what=" + what + ", extra=" + extra
                            + ", geracao=" + generation);
                    handleVideoFailure(failed);
                    return true;
                });

                mp.prepareAsync();
                Log.i(TAG, "MediaPlayer preparando video");
            } catch (Exception e) {
                Log.e(TAG, "Falha preparando video", e);
                handleVideoFailure(mp);
            }
        }

        private void handleVideoFailure(MediaPlayer failed) {
            if (failed != player) return;
            videoFailedThisSession = true;
            stopVideo();
            if (visible && hasValidSurface()) drawHome();
        }

        private void stopVideo() {
            if (player == null) return;
            MediaPlayer old = player;
            player = null;
            try {
                old.setOnPreparedListener(null);
                old.setOnErrorListener(null);
                old.setOnInfoListener(null);
            } catch (Exception ignored) { }
            try { old.release(); }
            catch (Exception e) { Log.w(TAG, "Falha liberando video", e); }
            Log.d(TAG, "Video parado e MediaPlayer liberado");
        }

        private void drawHome() {
            if (player != null || !hasValidSurface() || image == null) return;
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas == null) return;
                canvas.drawColor(0xFF160926);
                float ratio = (float) image.getWidth() / image.getHeight();
                float canvasRatio = (float) canvas.getWidth() / canvas.getHeight();
                Rect src;
                if (ratio > canvasRatio) {
                    int w = Math.round(image.getHeight() * canvasRatio);
                    int left = (image.getWidth() - w) / 2;
                    src = new Rect(left, 0, left + w, image.getHeight());
                } else {
                    int h = Math.round(image.getWidth() / canvasRatio);
                    int top = (image.getHeight() - h) / 2;
                    src = new Rect(0, top, image.getWidth(), top + h);
                }
                canvas.drawBitmap(image, src,
                        new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), paint);
            } catch (Exception e) {
                Log.e(TAG, "Falha renderizando imagem estatica", e);
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas); }
                    catch (Exception e) { Log.e(TAG, "Surface recusou imagem estatica", e); }
                }
            }
        }

        @Override
        public void onDestroy() {
            Log.i(TAG, "Engine encerrada");
            destroyed = true;
            handler.removeCallbacksAndMessages(null);
            if (receiverRegistered) {
                try { unregisterReceiver(screenReceiver); }
                catch (Exception e) { Log.w(TAG, "Falha ao remover receiver", e); }
                receiverRegistered = false;
            }
            stopVideo();
            if (image != null) {
                image.recycle();
                image = null;
            }
            super.onDestroy();
        }
    }
}
