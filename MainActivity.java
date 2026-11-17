package com.vicecityrog.wallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 72, 48, 48);
        layout.setBackgroundColor(0xFF160926);
        layout.setGravity(Gravity.CENTER);

        TextView heading = new TextView(this);
        heading.setText("VICE CITY ROG");
        heading.setTextSize(28);
        heading.setTextColor(0xFFFF73B9);
        heading.setGravity(Gravity.CENTER);
        layout.addView(heading);

        TextView text = new TextView(this);
        text.setText("Bloqueio: vídeo animado. Tela inicial: sua imagem GTA 6 estática. " +
            "A animação é interrompida quando não precisa aparecer.\n\n" +
            "Ao aplicar, escolha SOMENTE a tela de bloqueio se o ASUS oferecer a opção. " +
            "Caso apareça apenas 'Tela inicial e bloqueio', ambas serão configuradas, " +
            "mas este app tenta manter a home estática quando desbloqueado.\n\n" +
            "O comportamento exato depende da personalização do Android pela ASUS.");
        text.setTextSize(16);
        text.setTextColor(0xFFFFFFFF);
        text.setPadding(0, 32, 0, 32);
        layout.addView(text);

        Button button = new Button(this);
        button.setText("TESTAR E APLICAR WALLPAPER");
        button.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new ComponentName(this, ViceCityWallpaperService.class));
                startActivity(intent);
            } catch (Exception e) {
                // Algumas interfaces ASUS usam o seletor alternativo de wallpaper.
                Intent fallback = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
                try { startActivity(fallback); }
                catch (Exception ignored) {
                    android.widget.Toast.makeText(this,
                        "Abra Configurações > Papel de parede > Live Wallpapers",
                        android.widget.Toast.LENGTH_LONG).show();
                }
            }
        });
        layout.addView(button);
        setContentView(layout);
    }
}
