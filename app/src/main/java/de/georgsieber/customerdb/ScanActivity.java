package de.georgsieber.customerdb;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.Result;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import de.georgsieber.customerdb.importexport.CustomerVcfBuilder;
import de.georgsieber.customerdb.model.Customer;
import de.georgsieber.customerdb.tools.ColorControl;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScanActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private ScanActivity me;
    private CustomerDatabase mDb;
    private SharedPreferences mSettings;
    private ZXingScannerView mScannerView;
    private int currentCameraId = 0;

    private final static int CAMERA_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        me = this;

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // init settings
        mSettings = getSharedPreferences(MainActivity.PREFS_NAME, 0);

        // init database
        mDb = new CustomerDatabase(this);

        // init colors
        ColorControl.updateActionBarColor(this, mSettings);

        // init scanner
        mScannerView = findViewById(R.id.scannerView);
        mScannerView.setFlash(false);
        mScannerView.setAutoFocus(true);
        mScannerView.setAspectTolerance(0.5f);

        // request camera permission
        openCamera();
    }

    private void openCamera() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
        }
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case CAMERA_PERMISSION:
                if(grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.please_grant_camera_permission), Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //noinspection SwitchStatementWithTooFewBranches
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera(currentCameraId);
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera(); // Stop camera on pause
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    @Override
    public void handleResult(Result rawResult) {
        vibrate(this);

        String code = rawResult.getText();
        if(code == null) return;
        Log.i("SCAN", code);

        // check if code contains VCF formatted data
        try {
            InputStream stream = new ByteArrayInputStream(code.getBytes("UTF-8"));
            List<Customer> parsedCustomers = CustomerVcfBuilder.readVcfFile(new InputStreamReader(stream));
            if(parsedCustomers.size() > 0) {
                askImport(parsedCustomers);
                return;
            }
        } catch(Exception ignored) { }

        // check if code contains MECARD formatted data (similar to VCF, used by Huawei contacts app)
        if(code.startsWith("MECARD:")) {
            String formatted = code.substring(7);
            formatted = "BEGIN:VCARD\n" + TextUtils.join("\n", formatted.split(";")) + "\nEND:VCARD";
            try {
                InputStream stream = new ByteArrayInputStream(formatted.getBytes("UTF-8"));
                List<Customer> parsedCustomers = CustomerVcfBuilder.readVcfFile(new InputStreamReader(stream));
                if(parsedCustomers.size() > 0) {
                    askImport(parsedCustomers);
                    return;
                }
            } catch(Exception ignored) { }
        }

        // no valid data found
        Toast.makeText(this, getString(R.string.invalid_code), Toast.LENGTH_LONG).show();
        mScannerView.resumeCameraPreview(this);
    }

    private void askImport(final List<Customer> customers) {
        if(customers.size() == 0) { return; }

        // ask for import dialog
        String importDescription = "";
        for(Customer c : customers) {
            importDescription += c.getFirstLine() + "\n";
        }
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch(which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        for(Customer c : customers) {
                            mDb.addCustomer(c);
                        }
                        MainActivity.setUnsyncedChanges(me);
                        me.setResult(1);
                        me.finish();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        mScannerView.resumeCameraPreview(me);
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.do_you_want_to_import_this_customer) + "\n\n" + importDescription)
                .setPositiveButton(getString(R.string.yes), dialogClickListener)
                .setNegativeButton(getString(R.string.no), dialogClickListener)
                .setCancelable(false)
                .show();
    }

    static void vibrate(Context c) {
        Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (v != null) {
                v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } else {
            //deprecated in API 26
            if (v != null) {
                v.vibrate(100);
            }
        }
    }

}
