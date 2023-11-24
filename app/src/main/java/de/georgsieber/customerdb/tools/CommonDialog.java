package de.georgsieber.customerdb.tools;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;

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
        ad.show();
    }

    public static void exportFinishedDialog(final AppCompatActivity context, File f, String mimeType, String[] emailRecipients, String emailSubject, String emailText, ActivityResultLauncher<Intent> resultHandlerExportMoveFile) {
        AlertDialog ad = new AlertDialog.Builder(context).create();
        ad.setTitle(context.getResources().getString(R.string.export_ok));
        ad.setMessage(f.getPath());
        ad.setIcon(context.getResources().getDrawable(R.drawable.ic_tick_green_24dp));
        ad.setButton(DialogInterface.BUTTON_POSITIVE, context.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        ad.setButton(DialogInterface.BUTTON_NEUTRAL, context.getResources().getString(R.string.email), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // this opens app chooser instead of system email app
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_EMAIL, emailRecipients);
                intent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
                intent.putExtra(Intent.EXTRA_TEXT, emailText);
                if(f != null) {
                    intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, "de.georgsieber.customerdb.provider", f));
                }
                context.startActivity(Intent.createChooser(intent, context.getResources().getString(R.string.emailtocustomer)));
            }
        });
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            ad.setButton(DialogInterface.BUTTON_NEGATIVE, "Move", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.setType(mimeType);
                    intent.putExtra(Intent.EXTRA_TITLE, f.getName());
                    resultHandlerExportMoveFile.launch(intent);
                }
            });
        }
        ad.show();
    }

}
