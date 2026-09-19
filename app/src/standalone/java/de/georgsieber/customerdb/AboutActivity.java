package de.georgsieber.customerdb;

import android.annotation.SuppressLint;

import android.os.Bundle;
import android.view.View;

public class AboutActivity extends BaseAboutActivity {

    AboutActivity me;
    FeatureCheck mFc;

    private final static String UNLOCK_CODE_SHOP_URL = "https://georg-sieber.de/?page=app-customerdb#purchase";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        me = this;

        // init manual unlock
        mButtonDoSubCloud.setEnabled(true);
        mButtonDoSubCloud.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openBrowser(UNLOCK_CODE_SHOP_URL);
            }
        });
        mButtonDoBuyCommercialUse.setEnabled(true);
        mButtonDoBuyCommercialUse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openUnlockInputBox("systems.sieber.customerdb.cu", "cu", UNLOCK_CODE_SHOP_URL);
            }
        });
        mButtonDoBuyLargeCompany.setEnabled(true);
        mButtonDoBuyLargeCompany.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openUnlockInputBox("systems.sieber.customerdb.lc", "lc", UNLOCK_CODE_SHOP_URL);
            }
        });
        mButtonDoBuyInputOnlyMode.setEnabled(true);
        mButtonDoBuyInputOnlyMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openUnlockInputBox("systems.sieber.customerdb.iom", "iom", UNLOCK_CODE_SHOP_URL);
            }
        });
        mButtonDoBuyDesignOptions.setEnabled(true);
        mButtonDoBuyDesignOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openUnlockInputBox("systems.sieber.customerdb.do", "do", UNLOCK_CODE_SHOP_URL);
            }
        });
        mButtonDoBuyCustomFields.setEnabled(true);
        mButtonDoBuyCustomFields.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openUnlockInputBox("systems.sieber.customerdb.cf", "cf", UNLOCK_CODE_SHOP_URL);
            }
        });
        mButtonDoBuyFiles.setEnabled(true);
        mButtonDoBuyFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openUnlockInputBox("systems.sieber.customerdb.fs", "fs", UNLOCK_CODE_SHOP_URL);
            }
        });
        mButtonDoBuyCalendar.setEnabled(true);
        mButtonDoBuyCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openUnlockInputBox("systems.sieber.customerdb.cl", "cl", UNLOCK_CODE_SHOP_URL);
            }
        });
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
    }

}
