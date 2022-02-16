package de.georgsieber.customerdb;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingResult;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.SystemClock;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.georgsieber.customerdb.tools.ColorControl;
import de.georgsieber.customerdb.tools.CommonDialog;
import de.georgsieber.customerdb.tools.HttpRequest;


public class AboutActivity extends AppCompatActivity {

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

    AboutActivity me = this;

    private BillingClient mBillingClient;
    SharedPreferences mSettings;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // init settings
        mSettings = getSharedPreferences(MainActivity.PREFS_NAME, 0);

        // init activity view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        findViewById(R.id.imageVendorLogo).setOnClickListener(new DoubleClickListener() {
            @Override
            public void onDoubleClick() {
                openUnlockSelection();
            }
        });

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // init colors
        ColorControl.updateActionBarColor(this, mSettings);

        // get version
        String versionString = "v?";
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            versionString = String.format(getResources().getString(R.string.version), pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // set info label text
        ((TextView) findViewById(R.id.textViewVersion)).setText(
                versionString
        );

        // do feature check
        final FeatureCheck fc = new FeatureCheck(this);
        fc.setFeatureCheckReadyListener(new FeatureCheck.featureCheckReadyListener() {
            @Override
            public void featureCheckReady(boolean fetchSuccess) {
                if(fc.unlockedCommercialUsage) unlockPurchase("cu");
                if(fc.unlockedLargeCompany) unlockPurchase("lc");
                if(fc.unlockedInputOnlyMode) unlockPurchase("iom");
                if(fc.unlockedDesignOptions) unlockPurchase("do");
                if(fc.unlockedCustomFields) unlockPurchase("cf");
                if(fc.unlockedFiles) unlockPurchase("fs");
                if(fc.unlockedCalendar) unlockPurchase("cl");
                if(fc.activeSync) unlockPurchase("sync");
            }
        });
        fc.init();

        // init billing client
        mBillingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases()
                .setListener(new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
                int responseCode = billingResult.getResponseCode();
                if(responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for(Purchase purchase : purchases) {
                        if(purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                            if(purchase.getSku().equals("sync")) {
                                SharedPreferences.Editor editor = mSettings.edit();
                                editor.putString("sync-purchase-token", purchase.getPurchaseToken());
                                //Log.e("TOKEN", purchase.getPurchaseToken());
                                editor.apply();
                            }
                            unlockPurchase(purchase.getSku());
                            FeatureCheck.acknowledgePurchase(mBillingClient, purchase);
                        }
                    }
                } else if(responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                    CommonDialog.show(me,
                            getResources().getString(R.string.purchase_canceled),
                            getResources().getString(R.string.purchase_canceled_description),
                            CommonDialog.TYPE.WARN,
                            false
                    );
                } else if(responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                    try {
                        CommonDialog.show(me,
                            getResources().getString(R.string.purchase_already_done),
                            getResources().getString(R.string.purchase_already_done_description),
                            CommonDialog.TYPE.OK,
                            false
                        );
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        CommonDialog.show(me,
                                getResources().getString(R.string.purchase_failed),
                                getResources().getString(R.string.check_internet_conn),
                                CommonDialog.TYPE.FAIL,
                                false
                        );
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    querySkus();
                } else {
                    Snackbar.make(
                            findViewById(R.id.aboutMainView),
                            getResources().getString(R.string.store_not_avail) + " - " +
                                    getResources().getString(R.string.could_not_fetch_prices),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                CommonDialog.show(me,
                        getResources().getString(R.string.store_not_avail),
                        getResources().getString(R.string.check_internet_conn),
                        CommonDialog.TYPE.WARN,
                        true
                );
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBillingClient.endConnection();
    }

    private void unlockPurchase(String sku) {
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

    @SuppressLint("SetTextI18n")
    private void querySkus() {
        ArrayList<String> skuList = new ArrayList<>();
        skuList.add("cu");
        skuList.add("lc");
        skuList.add("iom");
        skuList.add("do");
        skuList.add("cf");
        skuList.add("fs");
        skuList.add("cl");
        SkuDetailsParams.Builder params = SkuDetailsParams
                .newBuilder()
                .setSkusList(skuList)
                .setType(BillingClient.SkuType.INAPP);
        mBillingClient.querySkuDetailsAsync(params.build(), new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(@NonNull BillingResult billingResult, @Nullable List<SkuDetails> skuDetailsList) {
                int responseCode = billingResult.getResponseCode();
                if(responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                    for(SkuDetails skuDetails : skuDetailsList) {
                        String sku = skuDetails.getSku();
                        String price = skuDetails.getPrice();
                        switch(sku) {
                            case "cu":
                                mSkuDetailsCommercialUse = skuDetails;
                                ((Button) findViewById(R.id.buttonBuyCommercialUse)).setText(price+"\n"+getResources().getString(R.string.buy_now));
                                ((Button) findViewById(R.id.buttonBuyCommercialUse)).setEnabled(true);
                                break;
                            case "lc":
                                mSkuDetailsLargeCompany = skuDetails;
                                ((Button) findViewById(R.id.buttonBuyLargeCompany)).setText(price+"\n"+getResources().getString(R.string.buy_now));
                                ((Button) findViewById(R.id.buttonBuyLargeCompany)).setEnabled(true);
                                break;
                            case "iom":
                                mSkuDetailsInputOnlyMode = skuDetails;
                                ((Button) findViewById(R.id.buttonBuyInputOnlyMode)).setText(price+"\n"+getResources().getString(R.string.buy_now));
                                ((Button) findViewById(R.id.buttonBuyInputOnlyMode)).setEnabled(true);
                                break;
                            case "do":
                                getmSkuDetailsDesignOptions = skuDetails;
                                ((Button) findViewById(R.id.buttonBuyDesignOptions)).setText(price+"\n"+getResources().getString(R.string.buy_now));
                                ((Button) findViewById(R.id.buttonBuyDesignOptions)).setEnabled(true);
                                break;
                            case "cf":
                                mSkuDetailsCustomFields = skuDetails;
                                ((Button) findViewById(R.id.buttonBuyCustomFields)).setText(price+"\n"+getResources().getString(R.string.buy_now));
                                ((Button) findViewById(R.id.buttonBuyCustomFields)).setEnabled(true);
                                break;
                            case "fs":
                                mSkuDetailsFiles = skuDetails;
                                ((Button) findViewById(R.id.buttonBuyFiles)).setText(price+"\n"+getResources().getString(R.string.buy_now));
                                ((Button) findViewById(R.id.buttonBuyFiles)).setEnabled(true);
                                break;
                            case "cl":
                                mSkuDetailsCalendar = skuDetails;
                                ((Button) findViewById(R.id.buttonBuyCalendar)).setText(price+"\n"+getResources().getString(R.string.buy_now));
                                ((Button) findViewById(R.id.buttonBuyCalendar)).setEnabled(true);
                                break;
                        }
                    }
                } else {
                    CommonDialog.show(me,
                            getResources().getString(R.string.store_not_avail),
                            getResources().getString(R.string.could_not_fetch_prices),
                            CommonDialog.TYPE.WARN,
                            false
                    );
                }
            }
        });

        ArrayList<String> skuList2 = new ArrayList<>();
        skuList2.add("sync");
        SkuDetailsParams.Builder params2 = SkuDetailsParams
                .newBuilder()
                .setSkusList(skuList2)
                .setType(BillingClient.SkuType.SUBS);
        mBillingClient.querySkuDetailsAsync(params2.build(), new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(@NonNull BillingResult billingResult, @Nullable List<SkuDetails> skuDetailsList) {
                int responseCode = billingResult.getResponseCode();
                if(responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                    for(SkuDetails skuDetails : skuDetailsList) {
                        String sku = skuDetails.getSku();
                        String price = skuDetails.getPrice();
                        switch(sku) {
                            case "sync":
                                mSkuDetailsSync = skuDetails;
                                ((Button) findViewById(R.id.buttonSubCloud)).setText(price+"\n"+getResources().getString(R.string.buy_now));
                                ((Button) findViewById(R.id.buttonSubCloud)).setEnabled(true);
                                break;
                        }
                    }
                }
            }
        });
    }

    @SuppressWarnings("UnusedReturnValue")
    private BillingResult doBuy(SkuDetails sku) {
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(sku)
                .build();
        return mBillingClient.launchBillingFlow(this, flowParams);
    }

    SkuDetails mSkuDetailsSync;
    SkuDetails mSkuDetailsCommercialUse;
    SkuDetails mSkuDetailsLargeCompany;
    SkuDetails mSkuDetailsInputOnlyMode;
    SkuDetails getmSkuDetailsDesignOptions;
    SkuDetails mSkuDetailsCustomFields;
    SkuDetails mSkuDetailsFiles;
    SkuDetails mSkuDetailsCalendar;

    public void doSubCloud(View v) {
        doBuy(mSkuDetailsSync);
    }
    public void doBuyCommercialUse(View v) {
        doBuy(mSkuDetailsCommercialUse);
    }
    public void doBuyLargeCompany(View v) {
        doBuy(mSkuDetailsLargeCompany);
    }
    public void doBuyInputOnlyMode(View v) {
        doBuy(mSkuDetailsInputOnlyMode);
    }
    public void doBuyDesignOptions(View v) {
        doBuy(getmSkuDetailsDesignOptions);
    }
    public void doBuyCustomFields(View v) {
        doBuy(mSkuDetailsCustomFields);
    }
    public void doBuyFiles(View v) {
        doBuy(mSkuDetailsFiles);
    }
    public void doBuyCalendar(View v) {
        doBuy(mSkuDetailsCalendar);
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

    private final static String ACTIVATE_URL = "https://apps.georg-sieber.de/activate/app.php";
    public void openUnlockSelection() {
        // collect all available inapp purchases
        final String[][] inappPurchases = new String[][] {
                new String[] {getString(R.string.calendar), "systems.sieber.customerdb.cl", "cl"},
                new String[] {getString(R.string.commercial_use), "systems.sieber.customerdb.cu", "cu"},
                new String[] {getString(R.string.custom_fields), "systems.sieber.customerdb.cf", "cf"},
                new String[] {getString(R.string.design_options), "systems.sieber.customerdb.do", "do"},
                new String[] {getString(R.string.files), "systems.sieber.customerdb.fs", "fs"},
                new String[] {getString(R.string.input_only_mode_inapp_title), "systems.sieber.customerdb.iom", "iom"},
                new String[] {getString(R.string.more_than_500_customers), "systems.sieber.customerdb.lc", "lc"}
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
                openUnlockInputBox(inappPurchases[which][1], inappPurchases[which][2]);
            }
        });
        builder.show();
    }
    private void openUnlockInputBox(final String requestFeature, final String sku) {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_input_box);
        ((TextView) ad.findViewById(R.id.textViewInputBox)).setText(R.string.unlock_code);
        ad.findViewById(R.id.buttonInputBoxOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                String text = ((EditText) ad.findViewById(R.id.editTextInputBox)).getText().toString().trim();
                HttpRequest hr = new HttpRequest(ACTIVATE_URL, null);
                ArrayList<HttpRequest.KeyValueItem> headers = new ArrayList<>();
                headers.add(new HttpRequest.KeyValueItem("X-Unlock-Feature",requestFeature));
                headers.add(new HttpRequest.KeyValueItem("X-Unlock-Code",text));
                hr.setRequestHeaders(headers);
                hr.setReadyListener(new HttpRequest.readyListener() {
                    @Override
                    public void ready(int statusCode, String responseBody) {
                        if(statusCode == 999 && responseBody.trim().equals("")) {
                            unlockPurchase(sku);
                            CommonDialog.show(me, getResources().getString(R.string.success), "", CommonDialog.TYPE.OK, false);
                        } else {
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
    public void onClickBallBreakAndroidApp(View v) {
        MainActivity.openPlayStore(this, "de.georgsieber.ballbreak");
    }
    public void onClickOco(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/schorschii/oco-server"));
        startActivity(browserIntent);
    }
    public void onClickMasterplan(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/schorschii/masterplan"));
        startActivity(browserIntent);
    }

}
