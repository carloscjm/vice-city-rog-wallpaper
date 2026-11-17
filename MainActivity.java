package com.vicecityrog.wallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Central do tema: wallpaper animado V3 + icon pack Vice City rosa neon.
 * O Android Launcher deve oferecer suporte a icon packs para aplica-los.
 * Nao tenta mudar icones de outros apps diretamente.
 */
public class MainActivity extends Activity {
    private static final int HOT_PINK = Color.rgb(255, 54, 183);
    private static final int PALE_PINK = Color.rgb(255, 198, 237);
    private static final int CYAN = Color.rgb(100, 225, 255);
    private LinearLayout content;

    private int dp(float n) {
        return (int) (getResources().getDisplayMetrics().density * n + 0.5f);
    }

    private TextView line(String value, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(dp(6), dp(5), dp(6), dp(6));
        return t;
    }

    private void addText(String value, float sp, int color, boolean bold) {
        content.addView(line(value, sp, color, bold));
    }

    private void addButton(String title, boolean primary, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(primary ? Color.WHITE : CYAN);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(primary ? HOT_PINK : Color.rgb(30, 14, 62));
        bg.setCornerRadius(dp(15));
        bg.setStroke(dp(2), primary ? PALE_PINK : CYAN);
        b.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(55));
        lp.setMargins(0, dp(9), 0, dp(6));
        content.addView(b, lp);
        b.setOnClickListener(click);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(19, 9, 42));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(22), dp(36), dp(22), dp(42));

        ImageView packLogo = new ImageView(this);
        packLogo.setImageResource(R.drawable.pack_preview);
        packLogo.setAdjustViewBounds(true);
        packLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(115), dp(115));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        content.addView(packLogo, logoParams);

        addText("VICE CITY ROG", 27, Color.WHITE, true);
        addText("TEMA COMPLETO  •  v4.0", 12, CYAN, true);
        addText("Wallpaper animado + icon pack rosa neon no mesmo aplicativo.",
            15, PALE_PINK, false);

        View line = new View(this);
        line.setBackgroundColor(HOT_PINK);
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(2));
        lineParams.setMargins(0, dp(24), 0, dp(18));
        content.addView(line, lineParams);

        addText("01  WALLPAPER", 20, Color.WHITE, true);
        addText("Bloqueio: animacao Vice City. Tela inicial: imagem estatica. " +
            "A animacao para quando nao esta visivel.", 15, PALE_PINK, false);
        addButton("Aplicar wallpaper", true, v -> openWallpaperPicker());

        addText("02  ICONES ROSA NEON", 20, Color.WHITE, true);
        addText("Os logotipos originais continuam reconheciveis. " +
            "O launcher aplica a moldura rosa neon aos icones suportados.",
            15, PALE_PINK, false);
        addButton("Ir para a tela inicial e aplicar icones", false, v -> goHome());
        addText("No ASUS Launcher: segure uma area vazia da home, abra " +
            "Preferencias / Pacote de icones e selecione 'Vice City ROG'. " +
            "Se essa opcao nao listar o pacote, pode ser necessario um " +
            "aplicador de icon packs compativel com ASUS.",
            13, Color.rgb(218, 211, 238), false);

        addText("Uma unica instalacao. Atualizacoes futuras modificam os dois componentes.",
            12, CYAN, false);

        scroll.addView(content);
        setContentView(scroll);
    }

    private void openWallpaperPicker() {
        try {
            Intent i = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                new ComponentName(this, ViceCityWallpaperService.class));
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));
            } catch (Exception fallback) {
                Toast.makeText(this, "Abra Configuracoes > Papel de parede", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void goHome() {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_HOME);
        try { startActivity(i); }
        catch (Exception e) {
            Toast.makeText(this, "Abra a tela inicial manualmente", Toast.LENGTH_SHORT).show();
        }
    }
}
