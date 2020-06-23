package de.georgsieber.customerdb;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;

import java.util.List;

class FeatureCheck {
    private BillingClient mBillingClient;
    private Context mContext;
    private SharedPreferences mSettings;
    boolean mUseCache = true;

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
        mBillingClient = BillingClient.newBuilder(mContext).setListener(new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
            }
        }).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
                if(billingResponseCode == BillingClient.BillingResponse.OK) {
                    // query purchases
                    if(mUseCache) {
                        //Log.i("PURCHASE", "Use cache");
                        Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
                        processPurchases(purchasesResult.getResponseCode(), purchasesResult.getPurchasesList());
                        Purchase.PurchasesResult subscriptionResult = mBillingClient.queryPurchases(BillingClient.SkuType.SUBS);
                        processSubscription(subscriptionResult.getResponseCode(), subscriptionResult.getPurchasesList());
                        isReady = true;
                    } else {
                        //Log.i("PURCHASE", "Use non-cache");
                        mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, new PurchaseHistoryResponseListener() {
                            @Override
                            public void onPurchaseHistoryResponse(int responseCode, List<Purchase> purchasesList) {
                                processPurchases(responseCode, purchasesList);
                                isReady = true;
                            }
                        });
                        mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS, new PurchaseHistoryResponseListener() {
                            @Override
                            public void onPurchaseHistoryResponse(int responseCode, List<Purchase> purchasesList) {
                                processSubscription(responseCode, purchasesList);
                            }
                        });
                    }
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

    private void processPurchases(int responseCode, List<Purchase> purchasesList) {
        if(responseCode == BillingClient.BillingResponse.OK) {
            for(Purchase p : purchasesList) {
                unlockPurchase(p.getSku());
            }
            processPurchasesResult = true;
        } else {
            processPurchasesResult = false;
        }
        finish();
    }
    private void processSubscription(int responseCode, List<Purchase> purchasesList) {
        if(responseCode == BillingClient.BillingResponse.OK) {
            for(Purchase p : purchasesList) {
                if(p.getSku().equals("sync")) {
                    SharedPreferences.Editor editor = mSettings.edit();
                    editor.putString("sync-purchase-token", p.getPurchaseToken());
                    editor.apply();
                    if(p.isAutoRenewing()) {
                        unlockPurchase("sync");
                    }
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

    private void unlockPurchase(String sku) {
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
                break;
        }
    }
}
