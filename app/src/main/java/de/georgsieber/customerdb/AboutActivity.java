package de.georgsieber.customerdb;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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

import java.util.ArrayList;
import java.util.List;

import de.georgsieber.customerdb.tools.ColorControl;
import de.georgsieber.customerdb.tools.CommonDialog;


public class AboutActivity extends AppCompatActivity {

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
                if(fc.activeSync) unlockPurchase("sync");
            }
        });
        fc.init();

        // init billing client
        mBillingClient = BillingClient.newBuilder(this).setListener(new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
                if(responseCode == BillingClient.BillingResponse.OK && purchases != null) {
                    for(Purchase purchase : purchases) {
                        if(purchase.getSku().equals("sync")) {
                            SharedPreferences.Editor editor = mSettings.edit();
                            editor.putString("sync-purchase-token", purchase.getPurchaseToken());
                            //Log.e("TOKEN", purchase.getPurchaseToken());
                            editor.apply();
                        }
                        unlockPurchase(purchase.getSku());
                    }
                } else if(responseCode == BillingClient.BillingResponse.USER_CANCELED) {
                    CommonDialog.show(me,
                            getResources().getString(R.string.purchase_canceled),
                            getResources().getString(R.string.purchase_canceled_description),
                            CommonDialog.TYPE.WARN,
                            false
                    );
                } else if(responseCode == BillingClient.BillingResponse.ITEM_ALREADY_OWNED) {
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
            public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
                if(billingResponseCode == BillingClient.BillingResponse.OK) {
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
        SkuDetailsParams.Builder params = SkuDetailsParams
                .newBuilder()
                .setSkusList(skuList)
                .setType(BillingClient.SkuType.INAPP);
        mBillingClient.querySkuDetailsAsync(params.build(), new SkuDetailsResponseListener() {
            @SuppressWarnings("RedundantCast")
            @Override
            public void onSkuDetailsResponse(int responseCode, List skuDetailsList) {
                if(responseCode == BillingClient.BillingResponse.OK && skuDetailsList != null) {
                    for(Object skuDetails : skuDetailsList) {
                        String sku = ((SkuDetails)skuDetails).getSku();
                        String price = ((SkuDetails)skuDetails).getPrice();
                        switch(sku) {
                            case "cu":
                                ((Button) findViewById(R.id.buttonBuyCommercialUse)).setText(price+"\n"+getResources().getString(R.string.buy_now));
                                ((Button) findViewById(R.id.buttonBuyCommercialUse)).setEnabled(true);
                                break;
                            case "lc":
                                ((Button) findViewById(R.id.buttonBuyLargeCompany)).setText(price+"\n"+getResources().getString(R.string.buy_now));
                                ((Button) findViewById(R.id.buttonBuyLargeCompany)).setEnabled(true);
                                break;
                            case "iom":
                                ((Button) findViewById(R.id.buttonBuyInputOnlyMode)).setText(price+"\n"+getResources().getString(R.string.buy_now));
                                ((Button) findViewById(R.id.buttonBuyInputOnlyMode)).setEnabled(true);
                                break;
                            case "do":
                                ((Button) findViewById(R.id.buttonBuyDesignOptions)).setText(price+"\n"+getResources().getString(R.string.buy_now));
                                ((Button) findViewById(R.id.buttonBuyDesignOptions)).setEnabled(true);
                                break;
                            case "cf":
                                ((Button) findViewById(R.id.buttonBuyCustomFields)).setText(price+"\n"+getResources().getString(R.string.buy_now));
                                ((Button) findViewById(R.id.buttonBuyCustomFields)).setEnabled(true);
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
            @SuppressWarnings("RedundantCast")
            @Override
            public void onSkuDetailsResponse(int responseCode, List skuDetailsList) {
                if(responseCode == BillingClient.BillingResponse.OK && skuDetailsList != null) {
                    for(Object skuDetails : skuDetailsList) {
                        String sku = ((SkuDetails) skuDetails).getSku();
                        String price = ((SkuDetails) skuDetails).getPrice();
                        switch(sku) {
                            case "sync":
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
    private int doBuy(String sku) {
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSku(sku)
                .setType(BillingClient.SkuType.INAPP)
                .build();
        return mBillingClient.launchBillingFlow(this, flowParams);
    }

    public void doSubCloud(View v) {
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSku("sync")
                .setType(BillingClient.SkuType.SUBS)
                .build();
        mBillingClient.launchBillingFlow(this, flowParams);
    }

    public void doBuyCommercialUse(View v) {
        doBuy("cu");
    }
    public void doBuyLargeCompany(View v) {
        doBuy("lc");
    }
    public void doBuyInputOnlyMode(View v) {
        doBuy("iom");
    }
    public void doBuyDesignOptions(View v) {
        doBuy("do");
    }
    public void doBuyCustomFields(View v) {
        doBuy("cf");
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

    public void onClickApacheLicenseLink(View v) {
        Intent licenseViewIntent = new Intent(this, ScriptActivity.class);
        licenseViewIntent.putExtra("content", "apache_license");
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
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/schorschii/customerdb-android"));
        startActivity(browserIntent);
    }

    public void onClickIosApp(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://apps.apple.com/us/app/customer-database/id1496659447"));
        startActivity(browserIntent);
    }

}
