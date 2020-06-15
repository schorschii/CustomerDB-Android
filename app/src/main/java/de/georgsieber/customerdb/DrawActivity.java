package de.georgsieber.customerdb;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.ByteArrayOutputStream;

public class DrawActivity extends AppCompatActivity {

    private DrawingView dv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // init activity view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);
        dv = findViewById(R.id.drawingView);

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_draw, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_done:
                Intent data = new Intent();
                data.putExtra("image", bitmapToBase64(bitmapFromView(dv)));
                setResult(RESULT_OK, data);
                finish();
                break;
            case R.id.action_abort:
                setResult(-1);
                finish();
                break;
            case R.id.action_clear:
                dv.mCanvas.drawColor(Color.WHITE);
                dv.invalidate();
                break;
        }
        return true;
    }

    private String bitmapToBase64(Bitmap b) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    public Bitmap bitmapFromView(View v) {
        Bitmap bitmap;
        v.setDrawingCacheEnabled(true);
        bitmap = Bitmap.createBitmap(v.getDrawingCache());
        v.setDrawingCacheEnabled(false);
        return bitmap;
    }
}
