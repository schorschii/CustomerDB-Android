package de.georgsieber.customerdb;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.List;

class FeatureCheck {

    /*  It is not allowed to modify this file in order to bypass license checks.
        I made this app open source hoping people will learn something from this project.
        But keep in mind: open source means free as "free speech" but not as in "free beer".
        Please be so kind and support further development by purchasing the in-app purchases in one of the app stores.
        It's up to you how long this app will be maintained. Thanks for your support.
    */

    private BillingClient mBillingClient;
    private Context mContext;
    private SharedPreferences mSettings;

    FeatureCheck(Context c) {
        mContext = c;
    }

    private featureCheckReadyListener listener = null;
    public interface featureCheckReadyListener {
        void featureCheckReady(boolean fetchSuccess);
    }
    void setFeatureCheckReadyListener(featureCheckReadyListener listener) {
        this.listener = listener;
    }

    void init() {
        // get settings (faster than google play - after purchase done, billing client needs minutes to realize the purchase)
        mSettings = mContext.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        unlockedCommercialUsage = mSettings.getBoolean("purchased-cu", false);
        unlockedAdFree = mSettings.getBoolean("purchased-ad", false);
        unlockedLargeCompany = mSettings.getBoolean("purchased-lc", false);
        unlockedLocalSync = mSettings.getBoolean("purchased-ls", false);
        unlockedInputOnlyMode = mSettings.getBoolean("purchased-iom", false);
        unlockedDesignOptions = mSettings.getBoolean("purchased-do", false);
        unlockedCustomFields = mSettings.getBoolean("purchased-cf", false);
        unlockedScript = mSettings.getBoolean("purchased-sc", false);
        unlockedFiles = mSettings.getBoolean("purchased-fs", false);
        unlockedCalendar = mSettings.getBoolean("purchased-cl", false);

        // init billing client - get purchases later for other devices
        mBillingClient = BillingClient.newBuilder(mContext)
                .enablePendingPurchases()
                .setListener(new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
            }
        }).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // query purchases
                    mBillingClient.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                            new PurchasesResponseListener() {
                                @Override
                                public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
                                    processPurchases(billingResult.getResponseCode(), list);
                                }
                            }
                    );
                    mBillingClient.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                            new PurchasesResponseListener() {
                                @Override
                                public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
                                    processSubscription(billingResult.getResponseCode(), list);
                                }
                            }
                    );
                    isReady = true;
                } else {
                    isReady = true;
                    if(listener != null) listener.featureCheckReady(false);
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
            }
        });
    }

    private Boolean processPurchasesResult = null;
    private Boolean processSubscriptionsResult = null;

    static void acknowledgePurchase(BillingClient client, Purchase purchase) {
        if(!purchase.isAcknowledged()) {
            AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();
            client.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                @Override
                public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) { }
            });
        }
    }

    private void processPurchases(int responseCode, List<Purchase> purchasesList) {
        if(responseCode == BillingClient.BillingResponseCode.OK) {
            for(Purchase p : purchasesList) {
                if(p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    for(String sku : p.getProducts()) {
                        unlockPurchase(sku, p);
                    }
                    acknowledgePurchase(mBillingClient, p);
                }
            }
            processPurchasesResult = true;
        } else {
            processPurchasesResult = false;
        }
        finish();
    }
    private void processSubscription(int responseCode, List<Purchase> purchasesList) {
        if(responseCode == BillingClient.BillingResponseCode.OK) {
            for(Purchase p : purchasesList) {
                if(p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    for(String sku : p.getProducts()) {
                        if(p.isAutoRenewing()) {
                            unlockPurchase(sku, p);
                        }
                    }
                    acknowledgePurchase(mBillingClient, p);
                }
            }
            processSubscriptionsResult = true;
        } else {
            processSubscriptionsResult = false;
        }
        finish();
    }

    private void finish() {
        if(processPurchasesResult != null && processSubscriptionsResult != null) {
            if(listener != null) {
                if(processPurchasesResult && processSubscriptionsResult) {
                    listener.featureCheckReady(true);
                } else {
                    listener.featureCheckReady(false);
                }
            }
        }
    }

    boolean isReady = false;

    boolean unlockedCommercialUsage = false;
    boolean unlockedLargeCompany = false;
    boolean unlockedInputOnlyMode = false;
    boolean unlockedDesignOptions = false;
    boolean unlockedCustomFields = false;
    boolean unlockedFiles = false;
    boolean unlockedCalendar = false;
    boolean activeSync = false;

    // deprecated
    private boolean unlockedAdFree = false;
    private boolean unlockedLocalSync = false;
    private boolean unlockedScript = false;

    private void unlockPurchase(String sku, Purchase purchase) {
        switch(sku) {
            case "cu":
                unlockedCommercialUsage = true;
                break;
            case "ad":
                unlockedAdFree = true;
                break;
            case "lc":
                unlockedLargeCompany = true;
                break;
            case "ls":
                unlockedLocalSync = true;
                break;
            case "iom":
                unlockedInputOnlyMode = true;
                break;
            case "do":
                unlockedDesignOptions = true;
                break;
            case "cf":
                unlockedCustomFields = true;
                break;
            case "sc":
                unlockedScript = true;
                break;
            case "fs":
                unlockedFiles = true;
                break;
            case "cl":
                unlockedCalendar = true;
                break;
            case "sync":
                activeSync = true;
                if(purchase != null) {
                    // store the sync subscription token in order to send it to the Customer Database Cloud API for access validation
                    SharedPreferences.Editor editor = mSettings.edit();
                    editor.putString("sync-purchase-token", purchase.getPurchaseToken());
                    editor.apply();
                }
                break;
        }
    }
}
