package de.georgsieber.customerdb.tools;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import de.georgsieber.customerdb.R;

public class CommonDialog {
    public enum TYPE {
        OK, WARN, FAIL, NONE
    }
    public static void show(final AppCompatActivity context, String title, String text, TYPE icon, final boolean finish) {
        if(context == null || context.isFinishing()) return;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if(context.isDestroyed()) return;
        }

        AlertDialog ad = new AlertDialog.Builder(context).create();
        ad.setCancelable(!finish);
        if(title != null && !title.equals("")) ad.setTitle(title);
        if(icon == TYPE.OK) {
            if(text != null && (!text.equals(""))) ad.setMessage(text);
            ad.setIcon(context.getResources().getDrawable(R.drawable.ic_tick_green_24dp));
        } else if(icon == TYPE.WARN) {
            if(text != null && (!text.equals(""))) ad.setMessage(text);
            ad.setIcon(context.getResources().getDrawable(R.drawable.ic_warning_orange_24dp));
        } else if(icon == TYPE.FAIL) {
            if(text != null && (!text.equals(""))) ad.setMessage(text);
            ad.setIcon(context.getResources().getDrawable(R.drawable.ic_fail_red_36dp));
        } else {
            ad.setMessage(text);
        }
        ad.setButton(context.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if(finish) context.finish();
            }
        });
        try {
            ad.show();
        } catch(WindowManager.BadTokenException ignored) {}
    }
}
