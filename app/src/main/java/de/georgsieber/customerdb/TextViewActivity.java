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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.georgsieber.customerdb.tools.ColorControl;

public class TextViewActivity extends AppCompatActivity {

    String mTitle = "";
    String mContent = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // init settings
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);

        // init activity view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_view);

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // init colors
        ColorControl.updateActionBarColor(this, settings);

        // load text
        mTitle = getIntent().getStringExtra("title");
        if(mTitle != null)
            this.setTitle(mTitle);

        mContent = getIntent().getStringExtra("content");
        if(mContent != null)
            ((TextView) findViewById(R.id.textViewScript)).setText(mContent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_text_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_send_via_email:
                if(mContent == null) break;
                sendViaEmail(mContent);
                break;
            case R.id.action_copy_to_clipboard:
                if(mContent == null) break;
                toClipboard(mContent);
                break;
        }
        return super.onOptionsItemSelected(item);
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

    public void sendViaEmail(String text) {
        File f = getStorageScript();
        try {
            FileOutputStream stream = new FileOutputStream(f);
            stream.write(text.getBytes());
            stream.close();
        } catch(IOException e) {
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

    private void toClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("phone", text);
        clipboard.setPrimaryClip(clip);

        Snackbar.make(findViewById(R.id.linearLayoutScriptActivityMainView), getResources().getString(R.string.copied_to_clipboard), Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .show();
    }

}
