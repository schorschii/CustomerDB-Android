package de.georgsieber.customerdb.tools;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

public class ColorControl {
    public final static int DEFAULT_COLOR_R = 15;
    public final static int DEFAULT_COLOR_G = 124;
    public final static int DEFAULT_COLOR_B = 157;

    public static int getColorFromSettings(SharedPreferences settings) {
        int r = settings.getInt("design-red", DEFAULT_COLOR_R);
        int g = settings.getInt("design-green", DEFAULT_COLOR_G);
        int b = settings.getInt("design-blue", DEFAULT_COLOR_B);
        return Color.argb(0xff,r,g,b);
    }

    public static void updateActionBarColor(AppCompatActivity a, SharedPreferences settings) {
        int color = getColorFromSettings(settings);
        ActionBar ab = a.getSupportActionBar();
        if(ab != null) ab.setBackgroundDrawable(new ColorDrawable(color));
        if(Build.VERSION.SDK_INT >= 21) {
            a.getWindow().setStatusBarColor(color);
        }
    }

    public static void updateAccentColor(View v, SharedPreferences settings) {
        int color = getColorFromSettings(settings);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            v.setBackgroundTintList(ColorStateList.valueOf(color));
        } else {
            v.setBackgroundDrawable(new ColorDrawable(color));
        }
    }

    public static boolean isColorDark(int color) {
        double darkness = 1-(0.299*Color.red(color) + 0.587*Color.green(color) + 0.114*Color.blue(color))/255;
        return !(darkness < 0.5);
    }

    public static String getHexColor(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }
}
