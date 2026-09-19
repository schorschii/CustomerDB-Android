package de.georgsieber.customerdb;

import android.content.Context;
import android.content.SharedPreferences;

class BaseFeatureCheck {

    /*  It is not allowed to modify this file in order to bypass license checks.
        I made this app open source hoping people will learn something from this project.
        But keep in mind: open source means free as "free speech" but not as in "free beer".
        Please be so kind and support further development by purchasing the in-app purchases in one of the app stores.
        It's up to you how long this app will be maintained. Thanks for your support.
    */

    protected Context mContext;
    protected SharedPreferences mSettings;

    BaseFeatureCheck(Context c) {
        mContext = c;
    }

    protected featureCheckReadyListener listener = null;
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

    protected void unlockPurchase(String sku) {
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
