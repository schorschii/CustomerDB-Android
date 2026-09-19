package de.georgsieber.customerdb;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.georgsieber.customerdb.tools.CommonDialog;

public class AboutActivity extends BaseAboutActivity {

    AboutActivity me;
    FeatureCheck mFc;
    BillingClient mBillingClient;

    ProductDetails mSkuDetailsSync;
    String mSkuDetailsSyncOfferToken;
    ProductDetails mSkuDetailsCommercialUse;
    ProductDetails mSkuDetailsLargeCompany;
    ProductDetails mSkuDetailsInputOnlyMode;
    ProductDetails getmSkuDetailsDesignOptions;
    ProductDetails mSkuDetailsCustomFields;
    ProductDetails mSkuDetailsFiles;
    ProductDetails mSkuDetailsCalendar;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        me = this;

        // init unlock
        mButtonDoSubCloud.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doBuy(mSkuDetailsSync, mSkuDetailsSyncOfferToken);
            }
        });
        mButtonDoBuyCommercialUse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doBuy(mSkuDetailsCommercialUse, null);
            }
        });
        mButtonDoBuyLargeCompany.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doBuy(mSkuDetailsLargeCompany, null);
            }
        });
        mButtonDoBuyInputOnlyMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doBuy(mSkuDetailsInputOnlyMode, null);
            }
        });
        mButtonDoBuyDesignOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doBuy(getmSkuDetailsDesignOptions, null);
            }
        });
        mButtonDoBuyCustomFields.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doBuy(mSkuDetailsCustomFields, null);
            }
        });
        mButtonDoBuyFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doBuy(mSkuDetailsFiles, null);
            }
        });
        mButtonDoBuyCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doBuy(mSkuDetailsCalendar, null);
            }
        });

        // init billing library
        loadPurchases();
    }

    private void loadPurchases() {
        // load in-app purchases
        mFc = new FeatureCheck(this);
        mFc.setFeatureCheckReadyListener(new FeatureCheck.featureCheckReadyListener() {
            @Override
            public void featureCheckReady(boolean fetchSuccess) {
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        if(mFc.unlockedCommercialUsage) unlockPurchase("cu");
                        if(mFc.unlockedLargeCompany) unlockPurchase("lc");
                        if(mFc.unlockedInputOnlyMode) unlockPurchase("iom");
                        if(mFc.unlockedDesignOptions) unlockPurchase("do");
                        if(mFc.unlockedCustomFields) unlockPurchase("cf");
                        if(mFc.unlockedFiles) unlockPurchase("fs");
                        if(mFc.unlockedCalendar) unlockPurchase("cl");
                        if(mFc.activeSync) unlockPurchase("sync");
                    }
                });
            }
        });
        mFc.init();

        // init Google billing client
        mBillingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .setListener(new PurchasesUpdatedListener() {
                    @Override
                    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
                        int responseCode = billingResult.getResponseCode();
                        if(responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                            for(final Purchase purchase : purchases) {
                                if(purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                    for(final String sku : purchase.getProducts()) {
                                        runOnUiThread(new Runnable(){
                                            @Override
                                            public void run() {
                                                unlockPurchase(sku);
                                                if(sku.equals("sync")) {
                                                    // store the sync subscription token in order to send it to the Customer Database Cloud API for access validation
                                                    SharedPreferences.Editor editor = mSettings.edit();
                                                    editor.putString("sync-purchase-token", purchase.getPurchaseToken());
                                                    editor.apply();
                                                }
                                            }
                                        });
                                    }
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
                        } else if(responseCode != BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
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
    @SuppressLint("SetTextI18n")
    private void querySkus() {
        ArrayList<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId("cu").setProductType(BillingClient.ProductType.INAPP).build());
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId("lc").setProductType(BillingClient.ProductType.INAPP).build());
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId("iom").setProductType(BillingClient.ProductType.INAPP).build());
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId("do").setProductType(BillingClient.ProductType.INAPP).build());
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId("cf").setProductType(BillingClient.ProductType.INAPP).build());
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId("fs").setProductType(BillingClient.ProductType.INAPP).build());
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId("cl").setProductType(BillingClient.ProductType.INAPP).build());
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();
        mBillingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull QueryProductDetailsResult queryProductDetailsResult) {
                if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for(final ProductDetails skuDetails : queryProductDetailsResult.getProductDetailsList()) {
                        final String sku = skuDetails.getProductId();
                        final String price = Objects.requireNonNull(skuDetails.getOneTimePurchaseOfferDetails()).getFormattedPrice();
                        runOnUiThread(new Runnable(){
                            @Override
                            public void run() {
                                setupPayButton(sku, price, skuDetails);
                            }
                        });
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

        ArrayList<QueryProductDetailsParams.Product> productList2 = new ArrayList<>();
        productList2.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId("sync").setProductType(BillingClient.ProductType.SUBS).build());
        QueryProductDetailsParams params2 = QueryProductDetailsParams.newBuilder()
                .setProductList(productList2)
                .build();
        mBillingClient.queryProductDetailsAsync(params2, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull QueryProductDetailsResult queryProductDetailsResult) {
                if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for(final ProductDetails skuDetails : queryProductDetailsResult.getProductDetailsList()) {
                        final String sku = skuDetails.getProductId();
                        Log.e("PURCHASE", sku);
                        final List<ProductDetails.SubscriptionOfferDetails> offers = Objects.requireNonNull(skuDetails.getSubscriptionOfferDetails());
                        if(offers.isEmpty()) continue;
                        mSkuDetailsSyncOfferToken = offers.get(0).getOfferToken();
                        final List<ProductDetails.PricingPhase> pricingPhaseList = offers.get(0).getPricingPhases().getPricingPhaseList();
                        if(pricingPhaseList.isEmpty()) continue;
                        final String price = pricingPhaseList.get(0).getFormattedPrice();
                        Log.e("PURCHASE", price);
                        runOnUiThread(new Runnable(){
                            @Override
                            public void run() {
                                setupPayButton(sku, price, skuDetails);
                            }
                        });
                    }
                }
            }
        });
    }
    @SuppressLint("SetTextI18n")
    private void setupPayButton(String sku, String price, ProductDetails skuDetails) {
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
            case "sync":
                mSkuDetailsSync = skuDetails;
                ((Button) findViewById(R.id.buttonSubCloud)).setText(price+"\n"+getResources().getString(R.string.buy_now));
                ((Button) findViewById(R.id.buttonSubCloud)).setEnabled(true);
                break;
        }
    }
    @SuppressWarnings("UnusedReturnValue")
    private BillingResult doBuy(ProductDetails sku, String offerToken) {
        if(sku == null) return null;
        BillingFlowParams.ProductDetailsParams.Builder builder = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(sku);
        if(offerToken != null) builder.setOfferToken(offerToken);
        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        productDetailsParamsList.add(builder.build());
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();
        return mBillingClient.launchBillingFlow(this, flowParams);
    }

}
