package de.georgsieber.customerdb;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.google.android.material.snackbar.Snackbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.georgsieber.customerdb.tools.Material3AppCompatActivity;

public class TextViewActivity extends Material3AppCompatActivity {

    String mTitle = "";
    String mContent = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_view);

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // load text
        mTitle = getIntent().getStringExtra("title");
        if(mTitle != null)
            this.setTitle(mTitle);

        mContent = getIntent().getStringExtra("content");
        if(mContent != null)
            ((TextView) findViewById(R.id.textViewScript)).setText(mContent);

        // apply the insets as a margin to the view, so that elements at the bottom
        // of the ScrollView do not get hidden behind the navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.spaceBottom), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.bottomMargin = insets.bottom;
            v.setLayoutParams(mlp);
            // Return CONSUMED if you don't want the window insets to keep passing down to descendant views.
            return WindowInsetsCompat.CONSUMED;
        });
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
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

        Snackbar.make(findViewById(R.id.scrollViewText), getResources().getString(R.string.copied_to_clipboard), Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .show();
    }

}
