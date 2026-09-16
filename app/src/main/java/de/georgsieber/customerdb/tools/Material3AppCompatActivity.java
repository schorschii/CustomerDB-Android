package de.georgsieber.customerdb.tools;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;

import de.georgsieber.customerdb.MainActivity;
import de.georgsieber.customerdb.R;

public class Material3AppCompatActivity extends AppCompatActivity {

    private SharedPreferences mSharedSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init edge-to-edge & action bar insets
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            EdgeToEdge.enable(this);
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                    .setAppearanceLightStatusBars(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mSharedSettings == null) {
            mSharedSettings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
            refreshActionBarColor();

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                AppBarLayout.LayoutParams mlp = (AppBarLayout.LayoutParams) v.getLayoutParams();
                mlp.topMargin = insets.top;
                v.setLayoutParams(mlp);
                // Return CONSUMED if you don't want the window insets to keep passing down to descendant views.
                return WindowInsetsCompat.CONSUMED;
            });
        }
    }

    protected void refreshActionBarColor() {
        if(mSharedSettings != null)
            findViewById(R.id.appBarLayout).setBackgroundColor(
                    ColorControl.getColorFromSettings(mSharedSettings)
            );
    }

}
