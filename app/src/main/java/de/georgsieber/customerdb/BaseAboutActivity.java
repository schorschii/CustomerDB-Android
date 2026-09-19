package de.georgsieber.customerdb;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.Toolbar;

import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import de.georgsieber.customerdb.tools.CommonDialog;
import de.georgsieber.customerdb.tools.HttpRequest;
import de.georgsieber.customerdb.tools.Material3AppCompatActivity;


public class BaseAboutActivity extends Material3AppCompatActivity {

    public static abstract class DoubleClickListener implements View.OnClickListener {
        // The time in which the second tap should be done in order to qualify as
        // a double click
        private static final long DEFAULT_QUALIFICATION_SPAN = 200;
        private long doubleClickQualificationSpanInMillis;
        private long timestampLastClick;

        public DoubleClickListener() {
            doubleClickQualificationSpanInMillis = DEFAULT_QUALIFICATION_SPAN;
            timestampLastClick = 0;
        }

        public DoubleClickListener(long doubleClickQualificationSpanInMillis) {
            this.doubleClickQualificationSpanInMillis = doubleClickQualificationSpanInMillis;
            timestampLastClick = 0;
        }

        @Override
        public void onClick(View v) {
            if((SystemClock.elapsedRealtime() - timestampLastClick) < doubleClickQualificationSpanInMillis) {
                onDoubleClick();
            }
            timestampLastClick = SystemClock.elapsedRealtime();
        }

        public abstract void onDoubleClick();
    }

    BaseAboutActivity me = this;

    SharedPreferences mSettings;

    Button mButtonDoSubCloud;
    Button mButtonDoBuyCommercialUse;
    Button mButtonDoBuyLargeCompany;
    Button mButtonDoBuyInputOnlyMode;
    Button mButtonDoBuyDesignOptions;
    Button mButtonDoBuyCustomFields;
    Button mButtonDoBuyFiles;
    Button mButtonDoBuyCalendar;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        findViewById(R.id.textViewVersion).setOnClickListener(new DoubleClickListener() {
            @Override
            public void onDoubleClick() {
                openUnlockSelection();
            }
        });

        // init settings
        mSettings = getSharedPreferences(MainActivity.PREFS_NAME, 0);

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // get version
        String versionString = "v?";
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            versionString = String.format(getResources().getString(R.string.version), pInfo.versionName);
        } catch(PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        ((TextView) findViewById(R.id.textViewVersion)).setText( versionString );

        // find views
        mButtonDoSubCloud = findViewById(R.id.buttonSubCloud);
        mButtonDoBuyCommercialUse = findViewById(R.id.buttonBuyCommercialUse);
        mButtonDoBuyLargeCompany = findViewById(R.id.buttonBuyLargeCompany);
        mButtonDoBuyInputOnlyMode = findViewById(R.id.buttonBuyInputOnlyMode);
        mButtonDoBuyDesignOptions = findViewById(R.id.buttonBuyDesignOptions);
        mButtonDoBuyCustomFields = findViewById(R.id.buttonBuyCustomFields);
        mButtonDoBuyFiles = findViewById(R.id.buttonBuyFiles);
        mButtonDoBuyCalendar = findViewById(R.id.buttonBuyCalendar);

        // show licensee
        String licensee = mSettings.getString("licensee", "");
        if(!licensee.isEmpty()) {
            findViewById(R.id.spaceLicensee).setVisibility(View.VISIBLE);
            findViewById(R.id.textViewLicensee).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.textViewLicensee)).setText(licensee);
        }

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void unlockPurchase(String sku) {
        SharedPreferences.Editor editor = mSettings.edit();
        switch(sku) {
            case "cu":
                ((ImageView) findViewById(R.id.imageViewBuyCommercialUse)).setImageResource(R.drawable.ic_tick_green_24dp);
                editor.putBoolean("purchased-cu", true);
                editor.apply();
                break;
            case "lc":
                ((ImageView) findViewById(R.id.imageViewBuyLargeCompany)).setImageResource(R.drawable.ic_tick_green_24dp);
                editor.putBoolean("purchased-lc", true);
                editor.apply();
                break;
            case "iom":
                ((ImageView) findViewById(R.id.imageViewBuyInputOnlyMode)).setImageResource(R.drawable.ic_tick_green_24dp);
                editor.putBoolean("purchased-iom", true);
                editor.apply();
                break;
            case "do":
                ((ImageView) findViewById(R.id.imageViewBuyDesignOptions)).setImageResource(R.drawable.ic_tick_green_24dp);
                editor.putBoolean("purchased-do", true);
                editor.apply();
                break;
            case "cf":
                ((ImageView) findViewById(R.id.imageViewBuyCustomFields)).setImageResource(R.drawable.ic_tick_green_24dp);
                editor.putBoolean("purchased-cf", true);
                editor.apply();
                break;
            case "fs":
                ((ImageView) findViewById(R.id.imageViewBuyFiles)).setImageResource(R.drawable.ic_tick_green_24dp);
                editor.putBoolean("purchased-fs", true);
                editor.apply();
                break;
            case "cl":
                ((ImageView) findViewById(R.id.imageViewBuyCalendar)).setImageResource(R.drawable.ic_tick_green_24dp);
                editor.putBoolean("purchased-cl", true);
                editor.apply();
                break;
            case "sync":
                ((ImageView) findViewById(R.id.imageViewBuySync)).setImageResource(R.drawable.ic_tick_green_24dp);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_settings_help:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.help_website)));
                startActivity(browserIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void openUnlockSelection() {
        // collect all available inapp purchases
        final String[][] inappPurchases = new String[][] {
                new String[] {getString(R.string.commercial_use), "systems.sieber.customerdb.cu", "cu"},
                new String[] {getString(R.string.more_than_500_customers), "systems.sieber.customerdb.lc", "lc"},
                new String[] {getString(R.string.input_only_mode_inapp_title), "systems.sieber.customerdb.iom", "iom"},
                new String[] {getString(R.string.design_options), "systems.sieber.customerdb.do", "do"},
                new String[] {getString(R.string.custom_fields), "systems.sieber.customerdb.cf", "cf"},
                new String[] {getString(R.string.files), "systems.sieber.customerdb.fs", "fs"},
                new String[] {getString(R.string.calendar), "systems.sieber.customerdb.cl", "cl"}
        };
        // generate name array for dialog
        ArrayList<String> names = new ArrayList<>();
        for(String[] s : inappPurchases) {
            names.add(s[0]);
        }
        // show selection dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.unlock));
        builder.setItems(names.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                openUnlockInputBox(inappPurchases[which][1], inappPurchases[which][2], null);
            }
        });
        builder.show();
    }
    protected void openUnlockInputBox(final String requestFeature, final String sku, final String shopUrl) {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_input_box);
        ((TextView) ad.findViewById(R.id.textViewInputBox)).setText(R.string.unlock_code);
        if(shopUrl != null) {
            ad.findViewById(R.id.buttonBuyCode).setVisibility(View.VISIBLE);
            ad.findViewById(R.id.buttonBuyCode).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openBrowser(shopUrl);
                }
            });
        }
        ad.findViewById(R.id.buttonInputBoxOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                String text = ((EditText) ad.findViewById(R.id.editTextInputBox)).getText().toString().trim();
                HttpRequest hr = new HttpRequest(getResources().getString(R.string.unlock_api), null);
                ArrayList<HttpRequest.KeyValueItem> headers = new ArrayList<>();
                headers.add(new HttpRequest.KeyValueItem("X-Unlock-Feature",requestFeature));
                headers.add(new HttpRequest.KeyValueItem("X-Unlock-Code",text));
                hr.setRequestHeaders(headers);
                hr.setReadyListener(new HttpRequest.readyListener() {
                    @Override
                    public void ready(int statusCode, String responseBody) {
                        try {
                            if(statusCode != 999) {
                                throw new Exception("Invalid status code: " + statusCode);
                            }
                            JSONObject licenseInfo = new JSONObject(responseBody);
                            String licensee = licenseInfo.getString("licensee");
                            String remaining = licenseInfo.getString("remaining");

                            final SharedPreferences.Editor editor = mSettings.edit();
                            editor.putString("licensee", licensee);
                            editor.apply();

                            unlockPurchase(sku);
                            CommonDialog.show(me,
                                    getResources().getString(R.string.success),
                                    licensee + "\n\n" + String.format(getString(R.string.activations_remaining), remaining),
                                    CommonDialog.TYPE.OK, false
                            );
                        } catch(Exception e) {
                            Log.e("ACTIVATION",  e.getMessage() + " - " + responseBody);
                            if(me == null || me.isFinishing()) return;
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                if(me.isDestroyed()) return;
                            }
                            CommonDialog.show(me, getResources().getString(R.string.error), getResources().getString(R.string.activation_failed_description), CommonDialog.TYPE.FAIL, false);
                        }
                    }
                });
                hr.execute();
            }
        });
        if(ad.getWindow() != null)
            ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        ad.show();
    }

    public void onClickApacheLicenseLink(View v) {
        try {
            InputStream in_s = getResources().openRawResource(R.raw.apache_license);
            byte[] b = new byte[in_s.available()];
            in_s.read(b);

            Intent licenseViewIntent = new Intent(this, TextViewActivity.class);
            licenseViewIntent.putExtra("content", new String(b));
            startActivity(licenseViewIntent);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    public void onClickMoreInfoBackup(View v) {
        Intent licenseViewIntent = new Intent(this, TextViewActivity.class);
        licenseViewIntent.putExtra("title", getString(R.string.backup));
        licenseViewIntent.putExtra("content", getString(R.string.backup_info));
        startActivity(licenseViewIntent);
    }
    public void onClickMoreInfoInputOnlyMode(View v) {
        Intent licenseViewIntent = new Intent(this, TextViewActivity.class);
        licenseViewIntent.putExtra("title", getString(R.string.input_only_mode));
        licenseViewIntent.putExtra("content", getString(R.string.input_only_mode_instructions));
        startActivity(licenseViewIntent);
    }
    public void onClickMoreInfoCardDavApi(View v) {
        Intent licenseViewIntent = new Intent(this, TextViewActivity.class);
        licenseViewIntent.putExtra("title", getString(R.string.carddav_api));
        licenseViewIntent.putExtra("content", getString(R.string.carddav_api_info));
        startActivity(licenseViewIntent);
    }
    public void onClickMoreInfoEula(View v) {
        Intent licenseViewIntent = new Intent(this, TextViewActivity.class);
        licenseViewIntent.putExtra("title", getString(R.string.eula_title));
        licenseViewIntent.putExtra("content", getString(R.string.eula));
        startActivity(licenseViewIntent);
    }

    public void onClickEmailLink(View v) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch(which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        final Intent emailIntent = new Intent(Intent.ACTION_VIEW);
                        Uri data = Uri.parse("mailto:"
                                + getResources().getString(R.string.developer_email)
                                + "?subject=" + getResources().getString(R.string.feedbacktitle)
                                + "&body=" + "");
                        emailIntent.setData(data);
                        startActivity(Intent.createChooser(emailIntent, getResources().getString(R.string.sendfeedback)));
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.support_only_english_german))
                .setPositiveButton(getString(R.string.cont), dialogClickListener)
                .setNegativeButton(getString(R.string.cancel), dialogClickListener)
                .show();
    }

    public void onClickWebLink(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.developer_website)));
        startActivity(browserIntent);
    }
    public void onClickGithub(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.repo_link)));
        startActivity(browserIntent);
    }

    public void onClickCustomerDatabaseIosApp(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://apps.apple.com/us/app/customer-database/id1496659447"));
        startActivity(browserIntent);
    }
    public void onClickRemotePointerAndroidApp(View v) {
        MainActivity.openPlayStore(this, "systems.sieber.remotespotlight");
    }
    public void onClickFsClockAndroidApp(View v) {
        MainActivity.openPlayStore(this, "systems.sieber.fsclock");
    }
    public void onClickVideoScreensaverAndroidApp(View v) {
        MainActivity.openPlayStore(this, "systems.sieber.vscreensaver");
    }
    public void onClickBallBreakAndroidApp(View v) {
        MainActivity.openPlayStore(this, "de.georgsieber.ballbreak");
    }
    public void onClickOco(View v) {
        openBrowser("https://github.com/schorschii/oco-server");
    }
    public void onClickMasterplan(View v) {
        openBrowser("https://github.com/schorschii/masterplan");
    }

    void openBrowser(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    String checkCode(String feature, String code) throws Exception {
        try {
            URL urlGetRequest = new URL(getResources().getString(R.string.unlock_api));
            HttpURLConnection conn = (HttpURLConnection) urlGetRequest.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-Unlock-Feature", feature);
            conn.setRequestProperty("X-Unlock-Code", code);
            int statusCode = conn.getResponseCode();
            conn.disconnect();
            if(statusCode == 999) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuffer response = new StringBuffer();
                String inputLine;
                while((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return response.toString();
            } else {
                throw new Exception(getString(R.string.invalid_code) + " ("+statusCode+")");
            }
        } catch(IOException e) {
            e.printStackTrace();
            throw new Exception(getString(R.string.check_internet_conn));
        }
    }

}
