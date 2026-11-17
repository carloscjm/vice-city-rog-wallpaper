package com.vicecityrog.wallpaper;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

/**
 * Live Wallpaper oficial do Android: animado somente quando o keyguard está ativo
 * (ou na tela de prévia do seletor). Sem serviço separado executando continuamente.
 */
public class ViceCityWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() { return new ViceCityEngine(); }

    private final class ViceCityEngine extends Engine {
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private final KeyguardManager keyguard =
            (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private MediaPlayer mediaPlayer;
        private Bitmap homeBitmap;
        private boolean visible = false;
        private boolean surfaceReady = false;
        private boolean receiverRegistered = false;
        private boolean preparing = false;
        private final Runnable refresh = this::updateRendering;

        private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                // Mudanças de estado são recebidas no thread principal.
                mainHandler.removeCallbacks(refresh);
                mainHandler.postDelayed(refresh, 100);
            }
        };

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(false);
            homeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.home);
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            filter.addAction(Intent.ACTION_USER_UNLOCKED);
            // Apenas broadcasts do sistema (Android 13+).
            registerReceiver(screenReceiver, filter);
            receiverRegistered = true;
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            surfaceReady = true;
            updateRendering();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            surfaceReady = true;
            updateRendering();
        }

        @Override
        public void onVisibilityChanged(boolean isVisible) {
            visible = isVisible;
            updateRendering();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            surfaceReady = false;
            stopVideo();
            super.onSurfaceDestroyed(holder);
        }

        private void updateRendering() {
            if (!surfaceReady || !visible) {
                stopVideo();
                return;
            }
            boolean locked = keyguard != null && keyguard.isKeyguardLocked();
            // O preview do Android também deve exibir a animação.
            if (locked || isPreview()) {
                startVideo();
            } else {
                stopVideo();
                drawHome();
            }
        }

        private void startVideo() {
            if (mediaPlayer != null || preparing) return;
            preparing = true;
            MediaPlayer player = new MediaPlayer();
            mediaPlayer = player;
            try {
                android.content.res.AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.vice_city_lock);
                if (afd == null) throw new IllegalStateException("Vídeo indisponível");
                try { player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength()); }
                finally { afd.close(); }
                player.setDisplay(getSurfaceHolder());
                player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                player.setVolume(0f, 0f);
                player.setLooping(true);
                player.setOnPreparedListener(mp -> {
                    preparing = false;
                    if (mediaPlayer != mp || !visible || !surfaceReady) return;
                    if (isPreview() || (keyguard != null && keyguard.isKeyguardLocked())) {
                        try { mp.start(); } catch (IllegalStateException ignored) { }
                    } else {
                        updateRendering();
                    }
                });
                player.setOnErrorListener((mp, what, extra) -> {
                    preparing = false;
                    stopVideo();
                    if (surfaceReady && visible) drawHome();
                    return true;
                });
                player.prepareAsync();
            } catch (Exception e) {
                preparing = false;
                stopVideo();
                drawHome();
            }
        }

        private void stopVideo() {
            preparing = false;
            if (mediaPlayer != null) {
                try { mediaPlayer.setOnPreparedListener(null); mediaPlayer.setOnErrorListener(null); }
                catch (Exception ignored) { }
                try { mediaPlayer.release(); } catch (Exception ignored) { }
                mediaPlayer = null;
            }
        }

        private void drawHome() {
            if (!surfaceReady || homeBitmap == null) return;
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas == null) return;
                canvas.drawColor(0xFF160926);
                float imageRatio = (float) homeBitmap.getWidth() / homeBitmap.getHeight();
                float canvasRatio = (float) canvas.getWidth() / canvas.getHeight();
                Rect src;
                if (imageRatio > canvasRatio) {
                    int cropW = Math.round(homeBitmap.getHeight() * canvasRatio);
                    int off = (homeBitmap.getWidth() - cropW) / 2;
                    src = new Rect(off, 0, off + cropW, homeBitmap.getHeight());
                } else {
                    int cropH = Math.round(homeBitmap.getWidth() / canvasRatio);
                    int off = (homeBitmap.getHeight() - cropH) / 2;
                    src = new Rect(0, off, homeBitmap.getWidth(), off + cropH);
                }
                canvas.drawBitmap(homeBitmap, src,
                    new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), paint);
            } catch (Exception ignored) {
                // A superfície pode desaparecer durante o desbloqueio.
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas); } catch (Exception ignored) { }
                }
            }
        }

        @Override
        public void onDestroy() {
            mainHandler.removeCallbacksAndMessages(null);
            if (receiverRegistered) {
                try { unregisterReceiver(screenReceiver); } catch (Exception ignored) { }
                receiverRegistered = false;
            }
            stopVideo();
            if (homeBitmap != null) {
                homeBitmap.recycle();
                homeBitmap = null;
            }
            super.onDestroy();
        }
    }
}
