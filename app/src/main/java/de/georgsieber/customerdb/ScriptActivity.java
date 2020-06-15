package de.georgsieber.customerdb;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import de.georgsieber.customerdb.tools.ColorControl;

public class ScriptActivity extends AppCompatActivity {

    String scriptContent = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // init settings
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);

        // init activity view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script);

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // init colors
        ColorControl.updateActionBarColor(this, settings);

        // load text
        String content = getIntent().getStringExtra("content");
        try {
            InputStream in_s = getResources().openRawResource(R.raw.apache_license);
            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            scriptContent = new String(b);
            ((TextView) findViewById(R.id.textViewScript)).setText(scriptContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private File getStorageScript() {
        File exportDir = new File(getExternalFilesDir(null), "tmp");
        exportDir.mkdirs();
        return new File(exportDir, "email.txt");
    }

    private void scanFile(File f) {
        Uri uri = Uri.fromFile(f);
        Intent scanFileIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
        sendBroadcast(scanFileIntent);
    }

    public void onSendViaEmailButtonClick(View v) {
        File f = getStorageScript();
        try {
            FileOutputStream stream = new FileOutputStream(f);
            stream.write(scriptContent.getBytes());
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        scanFile(f);

        Uri attachmentUri = FileProvider.getUriForFile(
                this,
                "de.georgsieber.customerdb.provider",
                f
        );
        // this opens app chooser instead of system email app
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name));
        intent.putExtra(Intent.EXTRA_TEXT, "");
        intent.putExtra(Intent.EXTRA_STREAM, attachmentUri);
        startActivity(Intent.createChooser(intent, getResources().getString(R.string.email)));
    }

    public void onCopyToClipboardButtonClick(View v) {
        toClipboard(this.scriptContent);
    }

    private void toClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("phone", text);
        clipboard.setPrimaryClip(clip);

        Snackbar.make(findViewById(R.id.linearLayoutScriptActivityMainView), getResources().getString(R.string.copied_to_clipboard), Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .show();
    }

}
